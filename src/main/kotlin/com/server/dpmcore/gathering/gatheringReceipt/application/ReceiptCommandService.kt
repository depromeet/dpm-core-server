package com.server.dpmcore.gathering.gatheringReceipt.application

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
    fun save(receipt: Receipt): Receipt = receiptPersistencePort.save(receipt)
}
