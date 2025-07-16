package com.server.dpmcore.bill.bill.persentation.dto.request

import java.time.LocalDateTime

data class CreateGatheringRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: String = "GATHERING",
    val receipt: CreateReceiptRequest,
    val gatheringMembers: MutableList<CreateGatheringMemberRequest>?,
)
