package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.member.aggregate.Member

interface GatheringV2InviteeCommandUseCase {
    fun createGatheringV2Invitee(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        authorMember: Member,
        inviteeMember: Member,
    )
}
