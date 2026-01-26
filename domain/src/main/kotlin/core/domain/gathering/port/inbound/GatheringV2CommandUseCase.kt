package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringV2InviteTag

interface GatheringV2CommandUseCase {
    fun createGatheringV2(
        gatheringV2: GatheringV2,
        gatheringV2InviteTags: List<GatheringV2InviteTag>,
    )
}
