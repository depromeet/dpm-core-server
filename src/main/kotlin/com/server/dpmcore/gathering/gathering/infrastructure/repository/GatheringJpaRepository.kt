package com.server.dpmcore.gathering.gathering.infrastructure.repository

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringJpaRepository : JpaRepository<GatheringEntity, Long> {
    fun findByBillId(billId: Long): List<GatheringEntity>
}
