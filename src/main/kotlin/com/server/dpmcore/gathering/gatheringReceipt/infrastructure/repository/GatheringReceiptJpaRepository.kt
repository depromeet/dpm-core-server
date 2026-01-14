package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringReceiptJpaRepository : JpaRepository<GatheringReceiptEntity, Long> {
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceiptEntity?
}
