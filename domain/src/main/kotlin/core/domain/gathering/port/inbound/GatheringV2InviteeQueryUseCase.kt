package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId

interface GatheringV2InviteeQueryUseCase {
    fun getInviteesByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee>

    fun getInviteeByMemberIdAndGatheringV2Id(
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    ): GatheringV2Invitee
}
