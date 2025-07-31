package com.server.dpmcore.gathering.gatheringMember.domain.port.inbound

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember

interface GatheringMemberQueryUseCase {
    fun getGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember>
}
