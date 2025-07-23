package com.server.dpmcore.gathering.gatheringReceipt.domain.port

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.ReceiptEntity

interface ReceiptPersistencePort {
    fun save(receipt: Receipt)

    fun findBy(gatheringReceiptId: Long): ReceiptEntity
}
