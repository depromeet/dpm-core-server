package core.domain.attendance.port.inbound.command

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant

data class AttendanceRecordCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val attendedAt: Instant,
    val attendanceCode: String,
)
