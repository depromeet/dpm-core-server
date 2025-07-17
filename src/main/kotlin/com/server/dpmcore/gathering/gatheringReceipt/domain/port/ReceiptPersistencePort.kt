package com.server.dpmcore.gathering.gatheringReceipt.domain.port

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt

interface ReceiptPersistencePort {
    fun save(receipt: Receipt): Receipt
}
