package core.application.afterParty.application.service.invitee

import core.application.afterParty.application.exception.member.AfterPartyMemberNotFoundException
import core.application.afterParty.presentation.response.AfterPartyRsvpMemberResponse
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.authority.MemberAuthorityService
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber
import org.springframework.stereotype.Service

@Service
class AfterPartyInviteeQueryService(
    val afterPartyInviteePersistencePort: AfterPartyInviteePersistencePort,
    val memberQueryService: MemberQueryService,
    val memberAuthorityService: MemberAuthorityService,
) : AfterPartyInviteeQueryUseCase {
    override fun getInviteesByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInvitee> =
        afterPartyInviteePersistencePort.findByAfterPartyId(afterPartyId)

    override fun getInviteeByMemberIdAndAfterPartyId(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): AfterPartyInvitee =
        afterPartyInviteePersistencePort.findByMemberIdAndAfterPartyId(
            memberId = memberId,
            afterPartyId = afterPartyId,
        )
            ?: throw AfterPartyMemberNotFoundException()

    fun getRsvpMembers(afterPartyId: AfterPartyId): List<AfterPartyRsvpMemberResponse> =
        getInviteesByAfterPartyId(afterPartyId).map { invitee ->
            val rsvpMember: Member = memberQueryService.getMemberById(invitee.memberId)
            val rsvpMemberTeamNumber: TeamNumber = memberQueryService.getMemberTeamNumber(invitee.memberId)
            val isAdmin: Boolean = memberAuthorityService.isAdmin(invitee.memberId)
            AfterPartyRsvpMemberResponse(
                memberId = rsvpMember.id ?: throw AfterPartyMemberNotFoundException(),
                name = rsvpMember.name,
                part = rsvpMember.part,
                teamNumber = rsvpMemberTeamNumber,
                isAdmin = isAdmin,
                rsvpStatus = invitee.rsvpStatus,
            )
        }
}
