package core.domain.attendance.aggregate

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceCreateCommand
import core.domain.attendance.vo.AttendanceId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant

/**
 * 출석(Attendance) 도메인 모델
 *
 * 출석은 특정 세션(Session)에 대한 디퍼의 출석 정보를 포함합니다.
 */
class Attendance(
    val id: AttendanceId? = null,
    val sessionId: SessionId,
    val memberId: MemberId,
    status: AttendanceStatus,
    attendedAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
    autoAbsentAt: Instant? = null,
) {
    var status: AttendanceStatus = status
        private set

    /**
     * 자동 결석 처리 시각. 자동 결석(스케줄러)으로 ABSENT 가 된 경우에만 기록되며,
     * 인증/운영진 변경/마감 연장에 따른 재개 시 해제됩니다. 기능 도입 전 기록은 null 입니다.
     */
    var autoAbsentAt: Instant? = autoAbsentAt
        private set

    var attendedAt: Instant? = attendedAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    /** 출석 상태가 PENDING가 아니고, 출석 시각이 존재하는지 여부를 확인합니다.*/
    fun isAttended(): Boolean = status != AttendanceStatus.PENDING && attendedAt != null

    /**
     * 운영진에 의해 이미 상태가 변경되었는지 여부를 확인합니다.
     *
     * updatedAt 은 운영진 변경(단건/일괄/결석 사유 승인)에서만 기록됩니다.
     * 자동 결석과 세션 정책 재계산은 updatedAt 을 기록하지 않습니다.
     * 과거 데이터에 updatedAt 이 있으면 출처와 관계없이 운영진 변경으로 보고 보호합니다.
     */
    fun isAlreadyUpdated(): Boolean = updatedAt != null

    /**
     * 자동 결석 처리로 결석이 된 상태인지 여부.
     *
     * 출처는 [autoAbsentAt] 표지로만 판단합니다. ABSENT 이면서 인증/운영진 변경 기록이 없더라도
     * 표지가 없는 기존 결석은 자동 결석으로 추정하지 않습니다.
     */
    fun isAutomaticallyAbsent(): Boolean =
        status == AttendanceStatus.ABSENT && autoAbsentAt != null && attendedAt == null && updatedAt == null

    /**
     * 출석 인증으로 상태를 기록할 수 있는지 여부.
     *
     * 미인증(PENDING) 이거나 자동 결석(표지 있음)인 경우만 가능합니다. 자동 결석은 마감 전에 접수된 유효한 인증이
     * 마감 처리보다 늦게 저장될 때 정상 판정으로 되돌리기 위해 허용합니다.
     * 운영진이 변경한 기록(updatedAt 존재)은 attendedAt 이 없어도 인증으로 덮어쓰지 않습니다.
     */
    fun canRecordAttendance(): Boolean =
        updatedAt == null &&
            attendedAt == null &&
            (status == AttendanceStatus.PENDING || isAutomaticallyAbsent())

    /** 자동 결석 대상(운영진 변경과 인증 기록이 없는 미인증) 여부 */
    fun isAutoAbsenceTarget(): Boolean = status == AttendanceStatus.PENDING && attendedAt == null && updatedAt == null

    /**
     * 출석 기록을 생성합니다.
     *
     * @param status 출석 상태
     * @param attendedAt 출석 시각
     *
     */
    fun markAttendance(
        status: AttendanceStatus,
        attendedAt: Instant,
    ) {
        this.status = status
        this.attendedAt = attendedAt
        this.autoAbsentAt = null
    }

    /** 운영진 변경. 운영진 변경 표지(updatedAt)를 기록하고 자동 결석 표지를 해제합니다. */
    fun updateStatus(
        newStatus: AttendanceStatus,
        updatedAt: Instant = Instant.now(),
    ) {
        this.status = newStatus
        this.updatedAt = updatedAt
        this.autoAbsentAt = null
    }

    fun delete(deletedAt: Instant?) {
        this.deletedAt = deletedAt
    }

    /**
     * 세션 출석 시각이 바뀌었을 때 이 기록이 가져야 할 새 상태를 계산합니다.
     *
     * 실제 반영과 변경 대상 미리보기가 같은 규칙을 쓰도록 이 함수 하나로 계산합니다.
     * - 운영진이 변경한 기록은 바꾸지 않습니다.
     * - 인증 기록이 있으면(PRESENT/LATE/ABSENT) 인증 시각으로 다시 판정합니다.
     * - 자동 결석은 마감이 연장되어 [now] 가 새 마감 전이면 다시 인증할 수 있도록 PENDING 으로 되돌립니다.
     * - 미인증(PENDING)은 그대로 두며, 새 마감이 지났다면 자동 결석 스케줄러가 처리합니다.
     *
     * @return 바뀌어야 할 새 상태. 바뀌지 않으면 null
     */
    fun recalculateStatusByPolicy(
        lateStart: Instant,
        absentStart: Instant,
        now: Instant,
    ): AttendanceStatus? {
        if (isAlreadyUpdated()) return null

        val attendedAt = this.attendedAt
        val newStatus =
            when {
                attendedAt != null && status in RECALCULABLE_ATTENDED_STATUSES ->
                    when {
                        attendedAt.isBefore(lateStart) -> AttendanceStatus.PRESENT
                        attendedAt.isBefore(absentStart) -> AttendanceStatus.LATE
                        else -> AttendanceStatus.ABSENT
                    }
                isAutomaticallyAbsent() && now.isBefore(absentStart) -> AttendanceStatus.PENDING
                else -> null
            }

        return newStatus?.takeIf { it != status }
    }

    /**
     * 정책 재계산 결과를 반영합니다. 운영진 변경 표지(updatedAt)는 기록하지 않습니다.
     * 재계산 결과는 인증 기록 재판정이거나 자동 결석 재개(PENDING)이므로 자동 결석 표지는 해제됩니다.
     */
    fun applyPolicyStatus(newStatus: AttendanceStatus) {
        this.status = newStatus
        this.autoAbsentAt = null
    }

    /** 자동 결석 처리. 운영진 변경 표지는 기록하지 않고 자동 결석 표지만 기록합니다. */
    fun markAutoAbsent(at: Instant) {
        this.status = AttendanceStatus.ABSENT
        this.autoAbsentAt = at
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attendance) return false
        return id == other.id && sessionId == other.sessionId && memberId == other.memberId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + memberId.hashCode()
        return result
    }

    override fun toString(): String = "Attendance(id=$id, sessionId=$sessionId, memberId=$memberId, status=$status)"

    companion object {
        private val RECALCULABLE_ATTENDED_STATUSES =
            setOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.ABSENT)

        fun create(command: AttendanceCreateCommand): Attendance =
            Attendance(
                sessionId = command.sessionId,
                memberId = command.memberId,
                status = AttendanceStatus.PENDING,
            )
    }
}
