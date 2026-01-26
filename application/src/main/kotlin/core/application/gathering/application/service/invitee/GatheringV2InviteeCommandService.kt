package core.application.gathering.application.service.invitee

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.inbound.GatheringV2InviteeCommandUseCase
import core.domain.gathering.port.inbound.GatheringV2InviteeQueryUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class GatheringV2InviteeCommandService(
    val gatheringV2InviteePersistencePort: GatheringV2InviteePersistencePort,
    val gatheringV2InviteeQueryUseCase: GatheringV2InviteeQueryUseCase,
) : GatheringV2InviteeCommandUseCase {
    override fun createGatheringV2Invitee(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        authorMember: Member,
        inviteeMember: Member,
    ) {
        gatheringV2InviteePersistencePort.save(
            gatheringV2Invitee,
            gatheringV2,
            authorMember,
            inviteeMember,
        )
    }

    override fun submitGatheringV2Participant(
        isParticipant: Boolean,
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    ) {
        val targetInvitee: GatheringV2Invitee =
            gatheringV2InviteeQueryUseCase.getInviteeByMemberIdAndGatheringV2Id(
                memberId = memberId,
                gatheringV2Id = gatheringV2Id,
            )
        val confirmedInvitee = targetInvitee.confirm(isParticipated = isParticipant)

        gatheringV2InviteePersistencePort.update(confirmedInvitee)
    }
}
