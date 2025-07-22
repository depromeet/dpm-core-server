package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringReceiptRepository(
    private val gatheringReceiptJpaRepository: GatheringReceiptJpaRepository,
) : GatheringReceiptPersistencePort {
    override fun save(
        gatheringReceipt: GatheringReceipt,
        gathering: Gathering,
    ) {
        gatheringReceiptJpaRepository.save(GatheringReceiptEntity.from(gatheringReceipt, gathering))
    }
}
