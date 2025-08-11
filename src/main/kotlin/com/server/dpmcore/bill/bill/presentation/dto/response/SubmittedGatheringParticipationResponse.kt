package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

class SubmittedGatheringParticipationResponse(
    val gatheringId: GatheringId,
    val isJoined: Boolean,
) {
    companion object {
        fun from(
            gatheringId: GatheringId,
            isJoined: Boolean,
        ): SubmittedGatheringParticipationResponse =
            SubmittedGatheringParticipationResponse(
                gatheringId = gatheringId,
                isJoined = isJoined,
            )
    }
}
