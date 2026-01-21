package com.server.dpmcore.gathering.gathering.presentation.response

import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel

data class GatheringMemberJoinListResponse(
    val members: List<GatheringMemberIsJoinQueryModel>,
) {
    companion object {
        fun from(members: List<GatheringMemberIsJoinQueryModel>): GatheringMemberJoinListResponse =
            GatheringMemberJoinListResponse(members)
    }
}
