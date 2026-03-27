package core.domain.bill.port.outbound.query

import core.domain.team.vo.TeamNumber

data class BillMemberIsInvitationSubmittedQueryModel(
    val name: String,
    val teamNumber: TeamNumber,
    val authority: String,
    val part: String?,
    val isInvitationSubmitted: Boolean,
)
