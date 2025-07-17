package com.server.dpmcore.bill.bill.presentation.dto.request

data class CreateBillRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billAccountId: Long,
    val inviteGroupIds: MutableList<Long>?,
    val gatherings: MutableList<CreateGatheringRequest>,
)
