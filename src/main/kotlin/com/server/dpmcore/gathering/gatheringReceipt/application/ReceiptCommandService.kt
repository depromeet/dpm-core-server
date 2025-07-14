package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.ReceiptRepositoryPort

class ReceiptCommandService(
    private val receiptRepositoryPort: ReceiptRepositoryPort,
) {
    fun save(receipt: Receipt): Receipt = receiptRepositoryPort.save(receipt)
}
