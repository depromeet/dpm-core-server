package com.server.dpmcore.gathering.gatheringReceipt.domain.port.inbound.command

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId

data class GatheringReceiptSplitAmountCommand(
    val gatheringReceiptId: Long,
    val splitAmount: Int?,
) {
    companion object {
        fun of(
            gatheringReceiptId: GatheringReceiptId,
            splitAmount: Int?,
        ) = GatheringReceiptSplitAmountCommand(
            gatheringReceiptId = gatheringReceiptId.value,
            splitAmount = splitAmount,
        )
    }
}
