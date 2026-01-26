package core.persistence.gathering.repository.invitee

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
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
        authorMember: Member,
        inviteeMember: Member,
    ) {
        gatheringV2InviteeJpaRepository.save(
            GatheringV2InviteeEntity.from(
                gatheringV2Invitee,
                GatheringV2Entity.of(
                    gatheringV2 = gatheringV2,
                    authorMember = MemberEntity.from(authorMember),
                ),
                MemberEntity.from(inviteeMember),
            ),
        )
    }

    override fun findByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee> =
        gatheringV2InviteeJpaRepository
            .findByGatheringId(
                gatheringV2Id,
            ).map {
                it.toDomain()
            }

    override fun findByMemberIdAndGatheringV2Id(
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    ): GatheringV2Invitee? =
        gatheringV2InviteeJpaRepository
            .findByMemberIdAndGatheringId(
                memberId = memberId,
                gatheringV2Id = gatheringV2Id,
            )?.toDomain()
}
