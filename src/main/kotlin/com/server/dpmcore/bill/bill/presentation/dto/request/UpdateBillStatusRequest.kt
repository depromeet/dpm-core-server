package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillStatus

data class UpdateBillStatusRequest(
    val status: BillStatus,
)
