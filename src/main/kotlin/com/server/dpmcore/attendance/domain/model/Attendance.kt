package com.server.dpmcore.attendance.domain.model

import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceCreateCommand
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import java.time.Instant

/**
 * 출석(Attendance) 도메인 모델
 * 출석은 특정 세션(Session)에 대한 디퍼의 출석 정보를 포함합니다.
 */
class Attendance internal constructor(
    val id: AttendanceId? = null,
    val sessionId: SessionId,
    val memberId: MemberId,
    status: AttendanceStatus,
    attendedAt: Instant? = null,
) {
    var status: AttendanceStatus = status
        private set

    var attendedAt: Instant? = attendedAt
        private set

    fun isAttended() = this.status != AttendanceStatus.PENDING

    fun markAttendance(
        status: AttendanceStatus,
        attendedAt: Instant,
    ) {
        this.status = status
        this.attendedAt = attendedAt
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
                sessionId = SessionId(command.sessionId),
                memberId = MemberId(command.memberId),
                status = AttendanceStatus.PENDING,
            )
    }
}
