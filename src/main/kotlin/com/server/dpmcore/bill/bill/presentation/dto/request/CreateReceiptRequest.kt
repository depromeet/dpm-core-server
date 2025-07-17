package com.server.dpmcore.bill.bill.presentation.dto.request

data class CreateReceiptRequest(
    val amount: Int,
    val receiptPhotos: MutableList<CreateReceiptPhotoRequest>?,
)
