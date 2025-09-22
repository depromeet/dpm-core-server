package core.domain.gathering.port.inbound

import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel
import core.domain.bill.vo.BillId
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.inbound.command.GatheringMemberReceiptQueryModel
import core.domain.gathering.port.outbound.query.SubmittedParticipantGathering
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId

interface GatheringQueryUseCase {
    fun getAllGatheringsByBillId(billId: BillId): List<Gathering>

    fun getAllGatheringIdsByBillId(billId: BillId): List<GatheringId>

    fun findGatheringReceiptByGatheringId(gatheringId: GatheringId): GatheringReceipt

    fun findGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember>

    fun getGatheringMemberReceiptByBillId(billId: BillId): List<GatheringMemberReceiptQueryModel>

    fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering>

    fun getBillMemberSubmittedList(billId: BillId): List<BillMemberIsInvitationSubmittedQueryModel>

    fun getAllGatheringMembersByBillId(billId: BillId): List<GatheringMember>

    fun findTotalSplitAmount(gatheringIds: List<GatheringId>): Int?

    fun getGatheringJoinMemberCount(
        gathering: Gathering,
        gatheringMembers: List<GatheringMember>,
    ): Int
}
