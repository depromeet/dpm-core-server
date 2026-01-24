package core.domain.gathering.port.outbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.vo.GatheringV2Id

interface GatheringV2PersistencePort {
    fun save(
        gatheringV2: GatheringV2,
    ): GatheringV2

    fun findById(gatheringV2Id: GatheringV2Id): List<GatheringV2>
}
