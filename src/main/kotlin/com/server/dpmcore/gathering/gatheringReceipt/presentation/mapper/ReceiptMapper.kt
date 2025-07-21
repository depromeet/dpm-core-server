package com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.ReceiptForBillCreateRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateReceiptResponse
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceiptPhoto.presentation.mapper.ReceiptPhotoMapper.toReceiptPhoto

object ReceiptMapper {
    fun toCreateReceiptResponse(receipt: Receipt): CreateReceiptResponse = CreateReceiptResponse()

    fun toReceipt(receiptForBillCreateRequest: ReceiptForBillCreateRequest): Receipt =
        Receipt(
            amount = receiptForBillCreateRequest.amount,
            receiptPhotos = receiptForBillCreateRequest.receiptPhotos?.map { toReceiptPhoto(it) }?.toMutableList(),
        )
}
