package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillId

data class BillPersistenceResponse(
    val billId: BillId,
)
