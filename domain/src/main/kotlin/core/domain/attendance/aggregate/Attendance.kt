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
) {
    var status: AttendanceStatus = status
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

    /** 미인증(PENDING)만 인증으로 기록할 수 있다. 운영진이 정한 기록은 덮어쓰지 않는다. */
    fun canRecordAttendance(): Boolean =
        updatedAt == null &&
            attendedAt == null &&
            status == AttendanceStatus.PENDING

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
    }

    /** 운영진 변경 표지(updatedAt)를 기록한다. */
    fun updateStatus(
        newStatus: AttendanceStatus,
        updatedAt: Instant = Instant.now(),
    ) {
        this.status = newStatus
        this.updatedAt = updatedAt
    }

    fun delete(deletedAt: Instant?) {
        this.deletedAt = deletedAt
    }

    /**
     * 출석 시각 변경 후의 새 상태(바뀌지 않으면 null). 미리보기와 실제 반영이 이 규칙 하나를 쓴다.
     * 인증 기록만 인증 시각으로 다시 판정하고, 운영진 변경 기록과 미인증은 그대로 둔다.
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
                else -> null
            }

        return newStatus?.takeIf { it != status }
    }

    /** 정책 재계산은 운영진 변경 표지(updatedAt)를 남기지 않는다. */
    fun applyPolicyStatus(newStatus: AttendanceStatus) {
        this.status = newStatus
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
