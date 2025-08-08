package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillId
import io.swagger.v3.oas.annotations.media.Schema

data class BillPersistenceResponse(
    @field:Schema(
        description = "정산 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billId: BillId,
)
