package core.domain.gathering.port.outbound.query

data class GatheringMemberIsJoinQueryModel(
    val name: String,
    val teamNumber: Int,
    val authority: String,
    val part: String?,
    val isJoined: Boolean?,
) {
    companion object {
        fun of(
            name: String,
            teamNumber: Int,
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
