package com.server.dpmcore.gathering.gatheringMember.infrastructure.repository

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.GatheringMemberPersistencePort
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringMemberRepository(
    private val gatheringJpaRepository: GatheringMemberJpaRepository,
) : GatheringMemberPersistencePort {
    override fun save(
        gatheringMember: GatheringMember,
        gathering: Gathering,
    ) {
        gatheringJpaRepository.save(GatheringMemberEntity.from(gatheringMember, gathering))
    }
}
