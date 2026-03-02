package core.application.afterParty.application.service.invitee

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.port.inbound.AfterPartyInviteeCommandUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AfterPartyInviteeCommandService(
    val afterPartyInviteePersistencePort: AfterPartyInviteePersistencePort,
    val afterPartyInviteeQueryUseCase: AfterPartyInviteeQueryUseCase,
) : AfterPartyInviteeCommandUseCase {
    override fun createAfterPartyInvitee(
        afterPartyInvitee: AfterPartyInvitee,
        afterParty: AfterParty,
        authorMember: Member,
        inviteeMember: Member,
    ) {
        afterPartyInviteePersistencePort.save(
            afterPartyInvitee,
            afterParty,
            authorMember,
            inviteeMember,
        )
    }

    override fun submitAfterPartyRsvpStatus(
        isRsvpGoing: Boolean,
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ) {
        val targetInvitee: AfterPartyInvitee =
            afterPartyInviteeQueryUseCase.getInviteeByMemberIdAndAfterPartyId(
                memberId = memberId,
                afterPartyId = afterPartyId,
            )
        val confirmedInvitee = targetInvitee.confirm(rsvpStatus = isRsvpGoing)

        afterPartyInviteePersistencePort.update(confirmedInvitee)
    }
}
