package com.server.dpmcore.gathering.gathering.application.query.model

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

data class SubmittedParticipantGathering(
    val gatheringId: GatheringId,
    val isJoined: Boolean,
)
