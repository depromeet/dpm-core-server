package core.domain.gathering.port.outbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.member.aggregate.Member

interface GatheringV2InviteePersistencePort {
    fun save(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        member: Member,
    )
}
