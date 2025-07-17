package com.server.dpmcore.bill.bill.presentation.dto.request

data class CreateReceiptPhotoRequest(
    val receiptId: Long,
    val photoUrl: String?,
)
