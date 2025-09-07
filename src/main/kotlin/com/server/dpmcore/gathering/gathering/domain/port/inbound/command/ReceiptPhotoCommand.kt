package com.server.dpmcore.gathering.gathering.domain.port.inbound.command

data class ReceiptPhotoCommand(
    val receiptId: Long,
    val photoUrl: String?,
)
