package core.domain.gathering.port.outbound

import core.domain.bill.aggregate.Bill
import core.domain.bill.vo.BillId
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.port.outbound.query.SubmittedParticipantGathering
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId

interface GatheringPersistencePort {
    fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering

    fun findByBillId(billId: BillId): List<Gathering>

    fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    )

    fun findAllGatheringIdsByBillId(billId: BillId): List<GatheringId>

    fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering>
}
