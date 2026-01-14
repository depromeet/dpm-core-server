package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.vo.GatheringId

interface GatheringMemberQueryUseCase {
    fun getGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember>
}
