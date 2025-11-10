package core.application.gathering.application.service.receipt

import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.inbound.GatheringReceiptQueryUseCase
import core.domain.gathering.port.outbound.GatheringReceiptPersistencePort
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class GatheringReceiptQueryService(
    private val gatheringReceiptPersistencePort: GatheringReceiptPersistencePort,
) : GatheringReceiptQueryUseCase {
    fun findById(gatheringReceiptId: GatheringReceiptId) = gatheringReceiptPersistencePort.findById(gatheringReceiptId)

    override fun findByGatheringId(gatheringId: GatheringId): GatheringReceipt =
        gatheringReceiptPersistencePort.findByGathering(gatheringId)
            ?: throw IllegalArgumentException("해당 모임의 영수증이 존재하지 않습니다. gatheringId: $gatheringId")

    fun getSplitAmountByGatheringId(gatheringId: GatheringId): Int? =
        gatheringReceiptPersistencePort.findSplitAmountByGatheringId(gatheringId)
}
