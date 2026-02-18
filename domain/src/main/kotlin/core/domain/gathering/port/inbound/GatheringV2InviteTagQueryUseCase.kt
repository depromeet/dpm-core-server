package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.vo.GatheringV2Id

interface GatheringV2InviteTagQueryUseCase {
    fun findByGatheringId(gatheringId: GatheringV2Id): List<GatheringV2InviteTag>

    fun findAllDistinct(): List<GatheringV2InviteTag>

    fun findDistinctByTagName(tagName: String): List<GatheringV2InviteTag>
}
