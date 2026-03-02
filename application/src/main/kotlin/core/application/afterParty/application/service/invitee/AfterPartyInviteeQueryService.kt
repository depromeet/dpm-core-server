package core.application.afterParty.application.service.invitee

import core.application.afterParty.application.exception.member.AfterPartyMemberNotFoundException
import core.application.afterParty.presentation.response.AfterPartyRsvpMemberResponse
import core.application.gathering.presentation.response.GatheringV2RsvpMemberResponse
import core.application.member.application.service.MemberQueryService
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AfterPartyInviteeQueryService(
    val afterPartyInviteePersistencePort: AfterPartyInviteePersistencePort,
    val memberQueryService: MemberQueryService,
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
            val rsvpMemberTeamId: Int = memberQueryService.getMemberTeamNumber(invitee.memberId)
            AfterPartyRsvpMemberResponse(
                memberId = rsvpMember.id ?: throw AfterPartyMemberNotFoundException(),
                name = rsvpMember.name,
                part = rsvpMember.part,
                team = rsvpMemberTeamId,
                rsvpStatus = invitee.rsvpStatus,
            )
        }

//    TODO : GatheringV2 삭제 예정
    @Deprecated(message = "GatheringV2에서만 사용되는 메서드입니다. GatheringV2 삭제 후 제거 예정입니다.")
    fun getRsvpMembersOld(afterPartyId: AfterPartyId): List<GatheringV2RsvpMemberResponse> =
        getInviteesByAfterPartyId(afterPartyId).map { invitee ->
            val rsvpMember: Member = memberQueryService.getMemberById(invitee.memberId)
            val rsvpMemberTeamId: Int = memberQueryService.getMemberTeamNumber(invitee.memberId)
            GatheringV2RsvpMemberResponse(
                memberId = rsvpMember.id ?: throw AfterPartyMemberNotFoundException(),
                name = rsvpMember.name,
                part = rsvpMember.part,
                team = rsvpMemberTeamId,
                rsvpStatus = invitee.rsvpStatus,
            )
        }
}
