package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.GatheringMemberQueryUseCase
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringMemberQueryService(
    private val gatheringMemberPersistencePort: GatheringMemberPersistencePort,
) : GatheringMemberQueryUseCase {
    override fun getGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringMemberPersistencePort.findByGatheringId(gatheringId)

    fun getGatheringMembersByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<GatheringMember> =
        gatheringMemberPersistencePort
            .findByGatheringIdAndMemberId(gatheringId, memberId)
            .takeIf { it.isNotEmpty() }
            ?: throw GatheringMemberException.GatheringMemberNotFoundException()
}
