package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringMemberJpaRepository : JpaRepository<GatheringMemberEntity, Long> {
    fun findByGatheringId(gatheringId: GatheringId): List<GatheringMemberEntity>
}
