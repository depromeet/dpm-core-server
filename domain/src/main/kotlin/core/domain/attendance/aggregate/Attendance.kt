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

    /** 자동 결석 출처 표지. 자동 결석에서만 기록하고 인증/운영진 변경/재개 시 지운다. 기능 도입 전 기록은 null. */
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

    /** updatedAt 은 운영진 변경에서만 기록한다. 과거 데이터의 updatedAt 도 운영진 변경으로 보고 보호한다. */
    fun isAlreadyUpdated(): Boolean = updatedAt != null

    /** 자동 결석 출처는 [autoAbsentAt] 표지로만 판단한다. 표지 없는 기존 결석은 자동 결석으로 추정하지 않는다. */
    fun isAutomaticallyAbsent(): Boolean =
        status == AttendanceStatus.ABSENT && autoAbsentAt != null && attendedAt == null && updatedAt == null

    /**
     * 미인증 또는 자동 결석만 인증으로 기록할 수 있다. 자동 결석은 마감 전에 접수된 인증이 늦게 저장될 때를 위해 허용한다.
     * 운영진이 정한 기록은 덮어쓰지 않는다.
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

    /** 운영진 변경 표지(updatedAt)를 기록하고 자동 결석 표지를 지운다. */
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
     * 출석 시각 변경 후의 새 상태(바뀌지 않으면 null). 미리보기와 실제 반영이 이 규칙 하나를 쓴다.
     * 인증 기록은 인증 시각으로 다시 판정하고, 자동 결석은 마감이 연장돼 [now] 가 새 마감 전이면 PENDING 으로 되돌린다.
     * 운영진 변경 기록과 미인증은 그대로 둔다.
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

    /** 정책 재계산은 updatedAt 을 남기지 않고 자동 결석 표지를 지운다. */
    fun applyPolicyStatus(newStatus: AttendanceStatus) {
        this.status = newStatus
        this.autoAbsentAt = null
    }

    /** updatedAt 은 남기지 않고 자동 결석 표지만 기록한다. */
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
