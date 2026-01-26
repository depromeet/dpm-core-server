package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id

interface GatheringV2InviteeQueryUseCase {
    fun getInviteesByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee>
}
