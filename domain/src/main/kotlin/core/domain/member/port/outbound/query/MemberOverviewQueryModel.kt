package core.domain.member.port.outbound.query

import core.domain.team.vo.TeamNumber

data class MemberOverviewQueryModel(
    val memberId: Long,
    val cohortId: Long?,
    val cohortValue: String?,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val status: String,
    val part: String?,
)
