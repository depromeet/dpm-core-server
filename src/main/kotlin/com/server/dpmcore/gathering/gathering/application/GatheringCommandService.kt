package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringCreateUseCase
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gatheringReceipt.application.ReceiptCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringCommandService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val receiptCommandService: ReceiptCommandService,
    private val billQueryUseCase: BillQueryUseCase,
) : GatheringCreateUseCase {
    override fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        billId: BillId,
    ) {
        try {
            val bill = billQueryUseCase.getById(billId)
            commands.forEach {
                val gathering = gatheringPersistencePort.save(bill, Gathering.create(it))
                receiptCommandService.saveReceiptDetails(it.receipt, gathering)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
