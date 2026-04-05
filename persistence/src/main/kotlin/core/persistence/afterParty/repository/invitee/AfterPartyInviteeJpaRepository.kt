package core.persistence.afterParty.repository.invitee

import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import core.entity.afterParty.AfterPartyInviteeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AfterPartyInviteeJpaRepository : JpaRepository<AfterPartyInviteeEntity, Long> {
    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteeEntity>

    fun findByMemberIdAndAfterPartyId(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): AfterPartyInviteeEntity?

    fun findByAfterPartyIdAndRsvpStatusIsNull(afterPartyId: AfterPartyId): List<AfterPartyInviteeEntity>
}
