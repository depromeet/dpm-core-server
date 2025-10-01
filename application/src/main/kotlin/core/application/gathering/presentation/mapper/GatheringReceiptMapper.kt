package core.application.gathering.presentation.mapper

import core.application.bill.presentation.request.ReceiptForBillCreateRequest
import core.domain.gathering.aggregate.GatheringReceipt

object GatheringReceiptMapper {
//    fun toCreateReceiptResponse(gatheringReceipt: GatheringReceipt): CreateReceiptResponse = CreateReceiptResponse()

    fun toReceipt(receiptForBillCreateRequest: ReceiptForBillCreateRequest): GatheringReceipt =
        GatheringReceipt(
            amount = receiptForBillCreateRequest.amount,
        )
}
