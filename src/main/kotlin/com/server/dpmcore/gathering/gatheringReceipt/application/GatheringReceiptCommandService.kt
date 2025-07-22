package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.ReceiptCommand
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptPersistencePort
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
    ) = gatheringReceiptPersistencePort.save(GatheringReceipt.create(receipt, gathering))
}
