package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringV2InviteTag

interface GatheringV2CommandUseCase {
    fun createGathering(
        gatheringV2: GatheringV2,
        gatheringV2InviteTag: GatheringV2InviteTag,
    )
}
