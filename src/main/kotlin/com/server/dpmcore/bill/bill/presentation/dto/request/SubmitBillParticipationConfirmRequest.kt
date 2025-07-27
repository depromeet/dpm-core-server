package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

data class SubmitBillParticipationConfirmRequest(
    val gatheringIds: List<GatheringId>,
)
