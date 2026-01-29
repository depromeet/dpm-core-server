package core.application.gathering.application.service.invitee

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.application.exception.member.GatheringMemberNotFoundException
import core.application.gathering.presentation.response.GatheringV2RsvpMemberResponse
import core.application.member.application.service.MemberQueryService
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.inbound.GatheringV2InviteeQueryUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class GatheringV2InviteeQueryService(
    val gatheringV2InviteePersistencePort: GatheringV2InviteePersistencePort,
    val memberQueryService: MemberQueryService,
) : GatheringV2InviteeQueryUseCase {
    override fun getInviteesByGatheringV2Id(gatheringV2Id: GatheringV2Id): List<GatheringV2Invitee> =
        gatheringV2InviteePersistencePort.findByGatheringV2Id(gatheringV2Id)

    override fun getInviteeByMemberIdAndGatheringV2Id(
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    ): GatheringV2Invitee =
        gatheringV2InviteePersistencePort.findByMemberIdAndGatheringV2Id(
            memberId = memberId,
            gatheringV2Id = gatheringV2Id,
        )
            ?: throw GatheringMemberNotFoundException()

    fun getRsvpMembers(gatheringV2Id: GatheringV2Id): List<GatheringV2RsvpMemberResponse> =
        getInviteesByGatheringV2Id(gatheringV2Id).map { invitee ->
            val rsvpMember: Member = memberQueryService.getMemberById(invitee.memberId)
            val rsvpMemberTeamId: Int = memberQueryService.getMemberTeamNumber(invitee.memberId)
            GatheringV2RsvpMemberResponse(
                memberId = rsvpMember.id ?: throw GatheringMemberNotFoundException(),
                name = rsvpMember.name,
                part = rsvpMember.part,
                team = rsvpMemberTeamId,
                isRsvpGoing = invitee.isRsvpGoing(),
            )
        }
}
