package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.inbound.GatheringV2InviteeQueryUseCase
import core.domain.gathering.port.inbound.GatheringV2QueryUseCase
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringV2QueryService(
    val gatheringV2PersistencePort: GatheringV2PersistencePort,
    val gatheringV2InviteeQueryUseCase: GatheringV2InviteeQueryUseCase,
) : GatheringV2QueryUseCase {
    fun getAllGatherings(memberId: MemberId): List<GatheringV2ListResponse> {
        val gatheringV2s: List<GatheringV2> = gatheringV2PersistencePort.findAll()

        return gatheringV2s.map { gatheringV2 ->
            val invitees =
                gatheringV2InviteeQueryUseCase.getInviteesByGatheringV2Id(
                    gatheringV2.id ?: throw GatheringNotFoundException(),
                )
            val isParticipated = invitees.find { it.memberId == memberId }?.isParticipated

            GatheringV2ListResponse.of(
                gatheringV2 = gatheringV2,
                gatheringV2Invitees = invitees,
                isParticipated = isParticipated,
                memberId = memberId,
            )
        }
    }
}
