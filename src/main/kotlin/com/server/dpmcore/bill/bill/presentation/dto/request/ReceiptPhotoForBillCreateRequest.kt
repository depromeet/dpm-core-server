package com.server.dpmcore.bill.bill.presentation.dto.request

data class ReceiptPhotoForBillCreateRequest(
    val receiptId: Long,
    val photoUrl: String?,
)
