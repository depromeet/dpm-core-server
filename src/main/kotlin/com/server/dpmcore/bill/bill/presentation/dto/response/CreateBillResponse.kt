package com.server.dpmcore.bill.bill.presentation.dto.response

import java.time.LocalDateTime

data class CreateBillResponse(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billTotalAmount: Long,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
//    val inviteGroups: MutableList<CreateInviteGroupResponse>,
    val gatherings: List<CreateGatheringResponse>,
)
