package core.persistence.gathering.repository.receipt

import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId
import core.entity.gathering.GatheringReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringReceiptJpaRepository : JpaRepository<GatheringReceiptEntity, Long> {
    fun findById(gatheringReceiptId: GatheringReceiptId): GatheringReceiptEntity?
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceiptEntity?
}
