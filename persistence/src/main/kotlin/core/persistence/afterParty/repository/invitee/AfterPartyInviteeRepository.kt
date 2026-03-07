package core.persistence.afterParty.repository.invitee

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import core.entity.afterParty.AfterPartyEntity
import core.entity.afterParty.AfterPartyInviteeEntity
import core.entity.member.MemberEntity
import org.springframework.stereotype.Repository

@Repository
class AfterPartyInviteeRepository(
    val afterPartyInviteeJpaRepository: AfterPartyInviteeJpaRepository,
) : AfterPartyInviteePersistencePort {
    override fun save(
        afterPartyInvitee: AfterPartyInvitee,
        afterParty: AfterParty,
        authorMember: Member,
        inviteeMember: Member,
    ) {
        afterPartyInviteeJpaRepository.save(
            AfterPartyInviteeEntity.from(
                afterPartyInvitee,
                AfterPartyEntity.of(
                    afterParty = afterParty,
                    authorMember = MemberEntity.Companion.from(authorMember),
                ),
                MemberEntity.from(inviteeMember),
            ),
        )
    }

    /**
     * 회식 참여 여부를 업데이트합니다.
     * afterPartyInvitee의 id를 기준으로 기존 엔티티를 조회한 후, 다른 연관 관계의 객체는 변경하지 않고
     * afterPartyInvitee의 값만 업데이트합니다.
     *
     * @param afterPartyInvitee 업데이트할 회식 초대자 정보
     * @return 없음
     * @since 2026-01-26
     * @author junwon
     */
    override fun update(afterPartyInvitee: AfterPartyInvitee) {
        val existingEntity =
            afterPartyInviteeJpaRepository.findById(afterPartyInvitee.id!!.value).get()
        afterPartyInviteeJpaRepository.save(existingEntity.from(afterPartyInvitee))
    }

    override fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInvitee> =
        afterPartyInviteeJpaRepository
            .findByAfterPartyId(
                afterPartyId,
            ).map {
                it.toDomain()
            }

    override fun findByMemberIdAndAfterPartyId(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): AfterPartyInvitee? =
        afterPartyInviteeJpaRepository
            .findByMemberIdAndAfterPartyId(
                memberId = memberId,
                afterPartyId = afterPartyId,
            )?.toDomain()
}
