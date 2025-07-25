package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillStatus
import java.time.LocalDateTime

data class BillListDetailResponse(
    val billId: Long,
    val title: String,
    val description: String? = null,
    val billTotalAmount: Int,
    val billStatus: BillStatus,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
    val inviteGroups: List<BillListInviteGroupDetailResponse>? = null,
    val answerMemberCount: Int = 0,
    val gatherings: List<BillListGatheringDetailResponse>,
)
