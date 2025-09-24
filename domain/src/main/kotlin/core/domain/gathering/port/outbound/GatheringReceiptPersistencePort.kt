package core.domain.gathering.port.outbound

import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId

interface GatheringReceiptPersistencePort {
    fun save(
        gatheringReceipt: GatheringReceipt,
        gathering: Gathering,
    )

    fun findById(gatheringReceiptId: GatheringReceiptId): GatheringReceipt?

    fun findByGathering(gatheringId: GatheringId): GatheringReceipt?

    fun updateSplitAmountById(gatheringReceipt: GatheringReceipt): Int?

    fun findSplitAmountByGatheringId(gatheringId: GatheringId): Int?
}
