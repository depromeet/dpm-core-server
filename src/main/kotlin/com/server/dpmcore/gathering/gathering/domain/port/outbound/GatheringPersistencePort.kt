package com.server.dpmcore.gathering.gathering.domain.port.outbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.model.query.SubmittedParticipantGathering
import com.server.dpmcore.member.member.domain.model.MemberId

interface GatheringPersistencePort {
    fun findGatheringById(id: Long): Gathering

    fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering

    fun findById(id: Long): Gathering

    fun findByBillId(billId: BillId): List<Gathering>

    fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    )

    fun findAllByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering>

    fun findAllGatheringIdsByBillId(billId: BillId): List<GatheringId>

    fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering>
}
