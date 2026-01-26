package core.domain.gathering.port.outbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId

interface GatheringV2InviteePersistencePort {
    fun save(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        authorMember: Member,
        inviteeMember: Member,
    )

    fun update(gatheringV2Invitee: GatheringV2Invitee)

    fun findByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee>

    fun findByMemberIdAndGatheringV2Id(
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    ): GatheringV2Invitee?
}
