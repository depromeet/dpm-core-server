package com.server.dpmcore.gathering.gatheringReceipt.domain.model

@JvmInline
value class ReceiptId(val value: Long) {
    override fun toString(): String = value.toString()
}
