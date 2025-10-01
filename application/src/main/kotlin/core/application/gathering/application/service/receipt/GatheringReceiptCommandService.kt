package core.application.gathering.application.service.receipt

import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.inbound.command.ReceiptCommand
import core.domain.gathering.port.outbound.GatheringReceiptPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringReceiptCommandService(
    private val gatheringReceiptPersistencePort: GatheringReceiptPersistencePort,
) {
    //    TODO 준원 : 영수증 사진 저장 로직 추가
    fun saveReceiptDetails(
        receipt: ReceiptCommand,
        gathering: Gathering,
    ) = gatheringReceiptPersistencePort.save(GatheringReceipt.create(receipt, gathering.id!!), gathering)

    fun updateSplitAmount(receipt: GatheringReceipt): Boolean =
        gatheringReceiptPersistencePort.updateSplitAmountById(receipt) != 0
}
