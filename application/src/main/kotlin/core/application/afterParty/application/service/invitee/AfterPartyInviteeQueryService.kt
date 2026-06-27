package core.application.afterParty.application.service.invitee

import core.application.afterParty.application.exception.member.AfterPartyMemberNotFoundException
import core.application.afterParty.presentation.response.AfterPartyRsvpMemberResponse
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.access.MemberAccessService
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AfterPartyInviteeQueryService(
    val afterPartyInviteePersistencePort: AfterPartyInviteePersistencePort,
    val memberQueryService: MemberQueryService,
    val memberAccessService: MemberAccessService,
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

    fun getRsvpMembers(afterPartyId: AfterPartyId): List<AfterPartyRsvpMemberResponse> {
        val invitees: List<AfterPartyInvitee> = getInviteesByAfterPartyId(afterPartyId = afterPartyId)
        val inviteeMember: List<Member> = memberQueryService.getMembersByIds(invitees.map { it.memberId })
        val rsvpMemberIds: List<MemberId> = inviteeMember.map { it.id!! }
        val rsvpMemberTeamNumberMap: Map<MemberId, TeamNumber> =
            memberQueryService.getMemberTeamNumberByMemberIds(
                rsvpMemberIds,
            )
        val retrievedMemberAdminMap: Map<MemberId, Boolean> =
            memberAccessService.getIsAdminByMemberIds(
                rsvpMemberIds,
            )
        val rsvpStatusByMemberId: Map<MemberId, Boolean?> = invitees.associate { it.memberId to it.rsvpStatus }

        return inviteeMember
            .map { member ->
                AfterPartyRsvpMemberResponse(
                    memberId = member.id!!,
                    name = member.name,
                    part = member.part,
                    teamNumber = rsvpMemberTeamNumberMap[member.id!!] ?: TeamNumber.defaultValue(),
                    isAdmin = retrievedMemberAdminMap[member.id!!] ?: false,
                    rsvpStatus = rsvpStatusByMemberId[member.id!!],
                )
            }.sortedWith(
                compareByDescending<AfterPartyRsvpMemberResponse> { it.rsvpStatus == true }
                    .thenBy { it.teamNumber.value == 0 }
                    .thenByDescending { it.rsvpStatus == false }
                    .thenByDescending { it.isAdmin }
                    .thenBy { it.teamNumber.value }
                    .thenBy { it.name },
            )
    }
}
