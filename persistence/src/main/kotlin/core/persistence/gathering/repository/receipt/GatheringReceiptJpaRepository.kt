package core.persistence.gathering.repository.receipt

import core.domain.gathering.vo.GatheringId
import core.entity.gathering.GatheringReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringReceiptJpaRepository : JpaRepository<GatheringReceiptEntity, Long> {
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceiptEntity?
}
