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

    /**
     * 회식 참여 여부를 업데이트합니다.
     * gatheringV2Invitee의 id를 기준으로 기존 엔티티를 조회한 후, 다른 연관 관계의 객체는 변경하지 않고
     * gatheringV2Invitee의 값만 업데이트합니다.
     *
     * @param gatheringV2Invitee 업데이트할 회식 초대자 정보
     * @return 없음
     * @since 2026-01-26
     * @author junwon
     */
    override fun update(gatheringV2Invitee: GatheringV2Invitee) {
        val existingEntity =
            gatheringV2InviteeJpaRepository.findById(gatheringV2Invitee.id!!.value).get()
        gatheringV2InviteeJpaRepository.save(existingEntity.from(gatheringV2Invitee))
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
