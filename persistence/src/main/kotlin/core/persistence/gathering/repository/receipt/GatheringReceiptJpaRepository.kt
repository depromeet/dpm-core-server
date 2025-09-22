package core.persistence.gathering.repository.receipt

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import core.persistence.gathering.entity.GatheringReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringReceiptJpaRepository : JpaRepository<GatheringReceiptEntity, Long> {
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceiptEntity?
}
