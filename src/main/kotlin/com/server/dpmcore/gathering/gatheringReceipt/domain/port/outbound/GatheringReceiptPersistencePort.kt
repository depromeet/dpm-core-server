package com.server.dpmcore.gathering.gatheringReceipt.domain.port.outbound

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceiptId
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.inbound.command.GatheringReceiptSplitAmountCommand
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity

interface GatheringReceiptPersistencePort {
    fun save(
        gatheringReceipt: GatheringReceipt,
        gathering: Gathering,
    )

    fun findById(gatheringReceiptId: GatheringReceiptId): GatheringReceiptEntity

    fun findByGathering(gatheringId: GatheringId): GatheringReceiptEntity

    fun updateSplitAmountById(command: GatheringReceiptSplitAmountCommand): Int

    fun findSplitAmountByGatheringId(gatheringId: GatheringId): Int?
}
