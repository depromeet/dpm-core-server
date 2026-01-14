package com.server.dpmcore.gathering.gathering.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.query.GatheringMemberReceiptQueryModel
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt

interface GatheringQueryUseCase {
    fun getAllGatheringsByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering>

    fun getAllGatheringsByBillId(billId: BillId): List<Gathering>

    fun getAllGatheringIdsByBillId(billId: BillId): List<GatheringId>

    fun findGatheringReceiptByGatheringId(gatheringId: GatheringId): GatheringReceipt

    fun findGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember>

    fun getGatheringMemberReceiptByBillId(billId: BillId): List<GatheringMemberReceiptQueryModel>
}
