package core.application.gathering.application.service.invitee

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.inbound.GatheringV2InviteeCommandUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.member.aggregate.Member
import org.springframework.stereotype.Service

@Service
class GatheringV2InviteeCommandService(
    val gatheringV2InviteePersistencePort: GatheringV2InviteePersistencePort,
) : GatheringV2InviteeCommandUseCase {
    override fun createGatheringV2Invitee(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        member: Member,
    ) {
        gatheringV2InviteePersistencePort.save(
            gatheringV2Invitee,
            gatheringV2,
            member,
        )
    }
}
