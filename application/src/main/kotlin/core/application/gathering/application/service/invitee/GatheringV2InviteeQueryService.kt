package core.application.gathering.application.service.invitee

import core.application.gathering.application.exception.GatheringNotFoundException
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.port.inbound.GatheringV2InviteeQueryUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteePersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class GatheringV2InviteeQueryService(
    val gatheringV2InviteePersistencePort: GatheringV2InviteePersistencePort,
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
            ?: throw GatheringNotFoundException()
}
