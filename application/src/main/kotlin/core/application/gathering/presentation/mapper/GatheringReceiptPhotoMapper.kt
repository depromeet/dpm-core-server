package core.application.gathering.presentation.mapper

import core.application.bill.presentation.request.ReceiptPhotoForBillCreateRequest
import core.domain.gathering.aggregate.GatheringReceiptPhoto

object GatheringReceiptPhotoMapper {
    fun toReceiptPhoto(receiptPhotoForBillCreateRequest: ReceiptPhotoForBillCreateRequest): GatheringReceiptPhoto =
        GatheringReceiptPhoto()
}
