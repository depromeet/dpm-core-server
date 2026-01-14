package core.domain.attendance.port.inbound.command

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class AttendanceCreateCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
)
