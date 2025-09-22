package core.persistence.gathering.repository

import core.entity.gathering.GatheringEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringJpaRepository : JpaRepository<GatheringEntity, Long> {
    fun findByBillId(billId: Long): List<GatheringEntity>
}
