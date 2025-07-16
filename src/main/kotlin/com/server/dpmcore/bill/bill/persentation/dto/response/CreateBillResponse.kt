package com.server.dpmcore.bill.bill.persentation.dto.response

import java.time.LocalDateTime

data class CreateBillResponse(
    val billId: Long,
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billTotalAmount: Long,
    val heldAt: LocalDateTime,
    val billAccountId: Long,
    val inviteGroups: MutableList<CreateInviteGroupResponse>,
    val gatherings: MutableList<CreateGatheringResponse>,
)
