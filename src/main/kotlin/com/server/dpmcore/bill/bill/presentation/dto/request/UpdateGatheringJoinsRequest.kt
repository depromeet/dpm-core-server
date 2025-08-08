package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import io.swagger.v3.oas.annotations.media.Schema

data class UpdateGatheringJoinsRequest(
    val gatheringJoins: List<EachGatheringJoin>,
) {
    data class EachGatheringJoin(
        @field:Schema(
            description = "참여하려는 회식 일련번호",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val gatheringId: GatheringId,
        @field:Schema(
            description = "참여 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val isJoined: Boolean,
    )
}
