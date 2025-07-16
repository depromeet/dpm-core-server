package com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper

import com.server.dpmcore.bill.bill.persentation.dto.request.CreateReceiptRequest
import com.server.dpmcore.bill.bill.persentation.dto.response.CreateReceiptResponse
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceiptPhoto.presentation.mapper.ReceiptPhotoMapper.toReceiptPhoto

object ReceiptMapper {
    fun toCreateReceiptResponse(receipt: Receipt): CreateReceiptResponse = CreateReceiptResponse()

    fun toReceipt(createReceiptRequest: CreateReceiptRequest): Receipt =
        Receipt(
            amount = createReceiptRequest.amount,
            receiptPhotos = createReceiptRequest.receiptPhotos?.map { toReceiptPhoto(it) }?.toMutableList(),
        )
}
