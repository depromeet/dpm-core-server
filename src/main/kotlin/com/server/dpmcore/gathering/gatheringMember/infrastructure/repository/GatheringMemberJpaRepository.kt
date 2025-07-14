package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringMemberJpaRepository : JpaRepository<GatheringMember, Long> {
    fun save(entity: GatheringMemberEntity): GatheringMemberEntity
}
