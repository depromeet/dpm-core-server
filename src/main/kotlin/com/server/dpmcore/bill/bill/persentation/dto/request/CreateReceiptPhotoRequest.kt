package com.server.dpmcore.bill.bill.persentation.dto.request

data class CreateReceiptPhotoRequest(
    val receiptId: Long,
    val photoUrl: String?,
)
