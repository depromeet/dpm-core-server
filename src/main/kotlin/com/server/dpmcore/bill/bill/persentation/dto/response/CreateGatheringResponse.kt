package com.server.dpmcore.bill.bill.persentation.dto.response

import java.time.LocalDateTime

data class CreateGatheringResponse(
    val title: String,
    val description: String?,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: String,
    val receipt: CreateReceiptResponse,
    val joinMemberCount: Int,
    val amount: Int,
    val gatheringMembers: MutableList<CreateGatheringMemberResponse>,
)
