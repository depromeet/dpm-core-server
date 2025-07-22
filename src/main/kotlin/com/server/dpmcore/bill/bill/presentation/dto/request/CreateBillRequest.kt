package com.server.dpmcore.bill.bill.presentation.dto.request

import jakarta.validation.constraints.NotEmpty

data class CreateBillRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billAccountId: Long,
    val inviteGroupIds: MutableList<Long>?,
    @NotEmpty val gatherings: MutableList<GatheringForBillCreateRequest>,
)
