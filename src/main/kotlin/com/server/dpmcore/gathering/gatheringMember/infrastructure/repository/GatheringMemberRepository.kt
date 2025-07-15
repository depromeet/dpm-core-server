package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.GatheringMemberPort
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringMemberRepository(
    private val gatheringJpaRepository: GatheringMemberJpaRepository,
) : GatheringMemberPort {
    override fun save(gatheringMember: GatheringMember): GatheringMember {
        return gatheringJpaRepository.save(GatheringMemberEntity.from(gatheringMember)).toDomain()
    }
}
