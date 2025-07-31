package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

data class UpdateGatheringJoinsRequest(
    val gatheringJoins: List<EachGatheringJoin>,
) {
    data class EachGatheringJoin(
        val gatheringId: GatheringId,
        val isJoined: Boolean,
    )
}
