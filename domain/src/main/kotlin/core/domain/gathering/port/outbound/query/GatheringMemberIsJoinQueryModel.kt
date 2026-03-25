package core.domain.gathering.port.outbound.query

import core.domain.team.vo.TeamNumber

data class GatheringMemberIsJoinQueryModel(
    val name: String,
    val teamNumber: TeamNumber,
    val authority: String,
    val part: String?,
    val isJoined: Boolean?,
) {
    companion object {
        fun of(
            name: String,
            teamNumber: TeamNumber,
            authority: String,
            part: String?,
            isJoined: Boolean?,
        ) = GatheringMemberIsJoinQueryModel(
            name = name,
            teamNumber = teamNumber,
            authority = authority,
            part = part,
            isJoined = isJoined,
        )
    }
}
