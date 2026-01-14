package core.application.gathering.presentation.response

import core.domain.gathering.port.outbound.query.GatheringMemberIsJoinQueryModel

data class GatheringMemberJoinListResponse(
    val members: List<GatheringMemberIsJoinQueryModel>,
) {
    companion object {
        fun from(members: List<GatheringMemberIsJoinQueryModel>): GatheringMemberJoinListResponse =
            GatheringMemberJoinListResponse(members)
    }
}
