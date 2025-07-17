package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.ReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.ReceiptEntity
import org.springframework.stereotype.Repository

@Repository
class ReceiptRepository(
    private val receiptJpaRepository: ReceiptJpaRepository,
) : ReceiptPersistencePort {
    override fun save(receipt: Receipt): Receipt = receiptJpaRepository.save(ReceiptEntity.from(receipt)).toDomain()
}
