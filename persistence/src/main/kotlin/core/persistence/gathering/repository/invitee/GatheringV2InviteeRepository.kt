package core.persistence.gathering.repository.invitee

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.member.aggregate.Member
import core.entity.gathering.GatheringV2Entity
import core.entity.gathering.GatheringV2InviteeEntity
import core.entity.member.MemberEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringV2InviteeRepository(
    val gatheringV2InviteeJpaRepository: GatheringV2InviteeJpaRepository,
) : GatheringV2InviteePersistencePort {
    override fun save(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        member: Member,
    ) {
        gatheringV2InviteeJpaRepository.save(
            GatheringV2InviteeEntity.from(
                gatheringV2Invitee,
                GatheringV2Entity.from(gatheringV2),
                MemberEntity.from(member),
            ),
        )
    }
}
