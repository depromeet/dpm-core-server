package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.GatheringMemberQueryUseCase
import com.server.dpmcore.gathering.gatheringMember.infrastructure.repository.GatheringMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringMemberQueryService(
    private val gatheringMemberRepository: GatheringMemberRepository,
) : GatheringMemberQueryUseCase {
    override fun getGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringMemberRepository.findByGatheringId(gatheringId)
}
