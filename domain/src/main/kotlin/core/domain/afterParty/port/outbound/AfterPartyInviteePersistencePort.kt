package core.domain.afterParty.port.outbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId

interface AfterPartyInviteePersistencePort {
    fun save(
        afterPartyInvitee: AfterPartyInvitee,
        afterParty: AfterParty,
        authorMember: Member,
        inviteeMember: Member,
    )

    fun update(afterPartyInvitee: AfterPartyInvitee)

    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInvitee>

    fun findByMemberIdAndAfterPartyId(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): AfterPartyInvitee?
}
