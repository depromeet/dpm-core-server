package com.server.dpmcore.gathering.gatheringReceipt.domain.port

import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt

interface GatheringReceiptQueryUseCase {
    fun findByGatheringId(gatheringId: GatheringId): GatheringReceipt
}
