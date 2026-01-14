package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.vo.GatheringId

interface GatheringReceiptQueryUseCase {
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceipt
}
