package core.domain.attendance.port.inbound.query

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class GetDetailAttendanceBySessionQuery(
    val sessionId: SessionId,
    val memberId: MemberId,
)
