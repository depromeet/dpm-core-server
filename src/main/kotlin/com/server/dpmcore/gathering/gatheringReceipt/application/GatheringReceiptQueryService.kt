package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class GatheringReceiptQueryService(
    private val gatheringReceiptPersistencePort: GatheringReceiptPersistencePort,
) {
    fun findBy(gatheringId: Long) = gatheringReceiptPersistencePort.findBy(gatheringId).toDomain()
}
