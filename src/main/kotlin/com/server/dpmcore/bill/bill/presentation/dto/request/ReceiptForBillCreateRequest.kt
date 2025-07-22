package com.server.dpmcore.bill.bill.presentation.dto.request

data class ReceiptForBillCreateRequest(
    val amount: Int,
    val receiptPhotos: MutableList<ReceiptPhotoForBillCreateRequest>?,
)
