package com.server.dpmcore.gathering.gatheringReceipt.domain.port

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt

interface GatheringReceiptPersistencePort {
    fun save(
        gatheringReceipt: GatheringReceipt,
        gathering: Gathering,
    )
}
