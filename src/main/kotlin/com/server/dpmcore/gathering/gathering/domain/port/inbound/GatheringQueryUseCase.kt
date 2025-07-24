package com.server.dpmcore.gathering.gathering.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId

interface GatheringQueryUseCase {
    fun getAllGatheringsByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering>

    fun findByBillId(billId: BillId): List<Gathering>
}
