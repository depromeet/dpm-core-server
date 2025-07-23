package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillStatus
import java.time.LocalDateTime

data class BillListDetailResponse(
    val billId: Long,
    val title: String,
    val description: String? = null,
//    TODO : 얘는 매핑을 어디서 시키는 게 좋은지 고민
    var billTotalAmount: Int,
    val billStatus: BillStatus,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
    val inviteGroups: List<BillListInviteGroupDetailResponse>? = null,
    val answerMemberCount: Int = 0,
    val gatherings: List<BillListGatheringDetailResponse>,
)
