package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.gathering.gathering.domain.model.GatheringCategory
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import java.time.LocalDateTime

data class BillListGatheringDetailResponse(
    val gatheringId: GatheringId,
    val title: String,
    val description: String? = null,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val category: GatheringCategory,
    val receipt: BillListGatheringReceiptDetailResponse? = null,
    val joinMemberCount: Int,
    val amount: Int,
    val splitAmount: Int,
)
