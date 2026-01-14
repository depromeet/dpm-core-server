package com.server.dpmcore.gathering.gatheringReceiptPhoto.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.ReceiptPhotoForBillCreateRequest
import com.server.dpmcore.gathering.gatheringReceiptPhoto.domain.model.GatheringReceiptPhoto

object GatheringReceiptPhotoMapper {
    fun toReceiptPhoto(receiptPhotoForBillCreateRequest: ReceiptPhotoForBillCreateRequest): GatheringReceiptPhoto =
        GatheringReceiptPhoto()
}
