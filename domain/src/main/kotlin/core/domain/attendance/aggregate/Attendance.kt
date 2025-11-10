package core.domain.attendance.aggregate

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceCreateCommand
import core.domain.attendance.vo.AttendanceId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant

/**
 * 출석(Attendance) 도메인 모델
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

    fun isAttended() = this.status != AttendanceStatus.PENDING

    fun markAttendance(
        status: AttendanceStatus,
        attendedAt: Instant,
    ) {
        this.status = status
        this.attendedAt = attendedAt
    }

    fun updateStatusByAttendancePolicy(lateStart: Instant, absentStart: Instant) {
        if (this.updatedAt != null) return

        val attendedAt = this.attendedAt ?: return

        val newStatus = when {
            attendedAt.isBefore(lateStart) -> AttendanceStatus.PRESENT
            attendedAt.isBefore(absentStart) -> AttendanceStatus.LATE
            else -> AttendanceStatus.ABSENT
        }

        if (this.status != newStatus) {
            this.status = newStatus
            this.updatedAt = Instant.now()
        }
    }

    fun updateStatus(status: AttendanceStatus) {
        this.status = status
        this.updatedAt = Instant.now()
    }

    fun delete(deletedAt: Instant?) {
        this.deletedAt = deletedAt
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
        fun create(command: AttendanceCreateCommand): Attendance =
            Attendance(
                sessionId = command.sessionId,
                memberId = command.memberId,
                status = AttendanceStatus.PENDING,
            )
    }
}
