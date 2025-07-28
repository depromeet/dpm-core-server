package com.server.dpmcore.gathering.gathering.domain.port.inbound

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.request.UpdateGatheringJoinsRequest
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId

interface GatheringCommandUseCase {
    fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        invitedAuthorityIds: List<AuthorityId>,
        billId: BillId,
    )

    fun updateGatheringReceiptSplitAmount(receipt: GatheringReceipt): Boolean

    fun markAsCheckedEachGatheringMember(
        gatheringIds: List<GatheringId>,
        memberId: MemberId,
    )

    fun markAsJoinedEachGatheringMember(
        request: UpdateGatheringJoinsRequest,
        memberId: MemberId,
    )

    fun submitBillParticipationConfirmEachGathering(
        billId: BillId,
        memberId: MemberId,
    )
}
