package core.domain.gathering.port.outbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member

interface GatheringV2InviteePersistencePort {
    fun save(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        authorMember: Member,
        inviteeMember: Member,
    )

    fun findByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee>
}
