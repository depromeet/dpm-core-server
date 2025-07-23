package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.ReceiptCommand
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.ReceiptPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReceiptCommandService(
    private val receiptPersistencePort: ReceiptPersistencePort,
) {
    //    TODO 준원 : 영수증 사진 저장 로직 추가
    fun saveReceiptDetails(
        receipt: ReceiptCommand,
        gathering: Gathering,
    ) = receiptPersistencePort.save(Receipt.create(receipt, gathering))

    fun save(receipt: Receipt) = receiptPersistencePort.save(receipt)
}
