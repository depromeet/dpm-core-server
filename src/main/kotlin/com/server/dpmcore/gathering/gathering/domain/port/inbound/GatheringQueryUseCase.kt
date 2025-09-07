package com.server.dpmcore.gathering.gathering.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.model.query.SubmittedParticipantGathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.query.GatheringMemberReceiptQueryModel
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId

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
}
