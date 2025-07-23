package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gatheringReceipt.domain.port.ReceiptPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ReceiptQueryService(
    private val receiptPersistencePort: ReceiptPersistencePort,
) {
    fun findBy(gatheringId: Long) = receiptPersistencePort.findBy(gatheringId).toDomain()
}
