package com.server.dpmcore.bill.bill.presentation.dto.request

import java.time.LocalDateTime

data class GatheringForBillCreateRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val receipt: ReceiptForBillCreateRequest,
)
