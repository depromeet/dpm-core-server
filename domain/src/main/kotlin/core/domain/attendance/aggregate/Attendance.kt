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

    /**
     * 출석 정책에 따라 상태를 업데이트합니다.
     *
     * @param lateStart 지각 시작 시각
     * @param absentStart 결석 시작 시각
     *
     */
    fun updateStatusByAttendancePolicy(
        lateStart: Instant,
        absentStart: Instant,
    ) {
        if (isAlreadyUpdated()) return

        val newStatus = calculateStatusByPolicy(lateStart, absentStart) ?: return
        if (status != newStatus) updateStatus(newStatus)
    }

    fun updateStatus(newStatus: AttendanceStatus) {
        this.status = newStatus
        this.updatedAt = Instant.now()
    }

    fun delete(deletedAt: Instant?) {
        this.deletedAt = deletedAt
    }

    /**
     * 새로운 정책 적용 시 상태 변경 여부 확인 합니다.
     *
     * @param lateStart 지각 시작 시각
     * @param absentStart 결석 시작 시각
     *
     */
    fun isStatusChangedByPolicy(
        lateStart: Instant,
        absentStart: Instant,
    ): Boolean {
        val newStatus = calculateStatusByPolicy(lateStart, absentStart) ?: return false
        return this.status != newStatus
    }

    /**
     * 정책 적용 시 예상 상태 계산 합니다.
     *
     * @param lateStart 지각 시작 시각
     * @param absentStart 결석 시작 시각
     *
     */
    fun simulateStatusChange(
        lateStart: Instant,
        absentStart: Instant,
    ): AttendanceStatus {
        return calculateStatusByPolicy(lateStart, absentStart) ?: this.status
    }

    /** 운영진에 의해 이미 상태가 변경되었는지 여부를 확인합니다. */
    fun isAlreadyUpdated(): Boolean = updatedAt != null

    /**
     * 새로운 출석 정책에 따라 출석 상태를 계산합니다.
     *
     * @param lateStart 지각 시작 시각
     * @param absentStart 결석 시작 시각
     *
     * @return AttendanceStatus 새로운 출석 상태 (nullable)
     *
     */
    private fun calculateStatusByPolicy(
        lateStart: Instant,
        absentStart: Instant,
    ): AttendanceStatus? {
        val attendedAt = this.attendedAt ?: return null

        return when {
            attendedAt.isBefore(lateStart) -> AttendanceStatus.PRESENT
            attendedAt.isBefore(absentStart) -> AttendanceStatus.LATE
            else -> AttendanceStatus.ABSENT
        }
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
