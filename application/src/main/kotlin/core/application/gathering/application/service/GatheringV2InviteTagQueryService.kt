package core.application.gathering.application.service

import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2InviteTagQueryUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteTagPersistencePort
import core.domain.gathering.vo.GatheringV2Id
import org.springframework.stereotype.Service

@Service
class GatheringV2InviteTagQueryService(
    private val gatheringV2InviteTagPersistencePort: GatheringV2InviteTagPersistencePort,
) : GatheringV2InviteTagQueryUseCase {
    override fun findByGatheringId(gatheringId: GatheringV2Id): List<GatheringV2InviteTag> =
        gatheringV2InviteTagPersistencePort.findByGatheringId(gatheringId)

    override fun findAllDistinct(): List<GatheringV2InviteTag> = gatheringV2InviteTagPersistencePort.findAllDistinct()

    override fun findDistinctByTagName(tagName: String): List<GatheringV2InviteTag> =
        gatheringV2InviteTagPersistencePort.findDistinctByTagName(tagName)
}