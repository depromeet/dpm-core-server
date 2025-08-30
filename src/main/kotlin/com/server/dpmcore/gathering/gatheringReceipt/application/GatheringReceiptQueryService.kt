package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class GatheringReceiptQueryService(
    private val gatheringReceiptPersistencePort: GatheringReceiptPersistencePort,
) : GatheringReceiptQueryUseCase {
    fun findById(gatheringReceiptId: GatheringReceiptId) =
        gatheringReceiptPersistencePort.findById(gatheringReceiptId).toDomain()

    override fun findByGatheringId(gatheringId: GatheringId): GatheringReceipt =
        gatheringReceiptPersistencePort.findByGathering(gatheringId).toDomain()

    fun getSplitAmountByGatheringId(gatheringId: GatheringId): Int? =
        gatheringReceiptPersistencePort.findSplitAmountByGatheringId(gatheringId)
}
