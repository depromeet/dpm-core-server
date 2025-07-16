package com.server.dpmcore.bill.bill.persentation.dto.request

data class CreateReceiptRequest(
    val amount: Int,
    val receiptPhotos: MutableList<CreateReceiptPhotoRequest>?,
)
