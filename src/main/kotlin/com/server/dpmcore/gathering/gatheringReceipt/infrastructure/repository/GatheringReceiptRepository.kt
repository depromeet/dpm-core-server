package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.springframework.data.repository.findByIdOrNull
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

    override fun findBy(gatheringReceiptId: Long): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByIdOrNull(
            gatheringReceiptId,
        ) ?: throw IllegalArgumentException("Receipt not found with id: $gatheringReceiptId")

    override fun findByGathering(gatheringId: GatheringId): GatheringReceiptEntity =
        gatheringReceiptJpaRepository.findByGatheringId(gatheringId.value)
}
