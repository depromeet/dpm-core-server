package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.ReceiptRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReceiptCommandService(
    private val receiptRepositoryPort: ReceiptRepositoryPort,
) {
//    TODO 준원 : 영수증 사진 저장 로직 추가
    fun save(receipt: Receipt): Receipt = receiptRepositoryPort.save(receipt)
}
