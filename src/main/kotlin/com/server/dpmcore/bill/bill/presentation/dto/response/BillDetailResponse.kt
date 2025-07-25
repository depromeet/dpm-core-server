package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillStatus
import java.time.LocalDateTime

data class BillDetailResponse(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billTotalAmount: Int,
    val billTotalSplitAmount: Int,
    val billStatus: BillStatus,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
//    val inviteGroups: MutableList<CreateInviteGroupResponse>,
    val gatherings: List<BillDetailGatheringResponse>,
)
