package com.server.dpmcore.gathering.gatheringReceiptPhoto.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.CreateReceiptPhotoRequest
import com.server.dpmcore.gathering.gatheringReceiptPhoto.domain.model.ReceiptPhoto

object ReceiptPhotoMapper {
    fun toReceiptPhoto(createReceiptPhotoRequest: CreateReceiptPhotoRequest): ReceiptPhoto = ReceiptPhoto()
}
