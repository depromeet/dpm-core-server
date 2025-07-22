package com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.ReceiptForBillCreateRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateReceiptResponse
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt

object GatheringReceiptMapper {
    fun toCreateReceiptResponse(gatheringReceipt: GatheringReceipt): CreateReceiptResponse = CreateReceiptResponse()

    fun toReceipt(receiptForBillCreateRequest: ReceiptForBillCreateRequest): GatheringReceipt =
        GatheringReceipt(
            amount = receiptForBillCreateRequest.amount,
        )
}
