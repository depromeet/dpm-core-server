package core.domain.absencereason.port.inbound.command

import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class AbsenceReportCreateCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val contents: String,
)
