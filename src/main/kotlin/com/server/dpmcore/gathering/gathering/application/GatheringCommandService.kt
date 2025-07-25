package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringCommandUseCase
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberCommandService
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberQueryService
import com.server.dpmcore.gathering.gatheringReceipt.application.GatheringReceiptCommandService
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.QueryMemberByAuthorityUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringCommandService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val gatheringReceiptCommandService: GatheringReceiptCommandService,
    private val gatheringMemberCommandService: GatheringMemberCommandService,
    private val gatheringMemberQueryService: GatheringMemberQueryService,
    private val queryMemberByAuthorityUseCase: QueryMemberByAuthorityUseCase,
    private val billQueryUseCase: BillQueryUseCase,
) : GatheringCommandUseCase {
    override fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        invitedAuthorityIds: List<AuthorityId>,
        billId: BillId,
    ) {
        val bill = billQueryUseCase.getById(billId)
        val memberIds =
            queryMemberByAuthorityUseCase.findAllMemberIdByAuthorityIds(invitedAuthorityIds)
        commands.forEach {
            val gatheringObject = saveGatheringObject(bill, it)
            saveEachGatheringReceiptAndAttachPhoto(it, gatheringObject)
            saveEachGatheringMembersByInvitedAuthority(memberIds, gatheringObject)
        }
    }

    private fun saveEachGatheringMembersByInvitedAuthority(
        memberIds: List<MemberId>,
        gatheringObject: Gathering,
    ) {
        gatheringMemberCommandService.saveEachGatheringMembers(
            memberIds,
            gatheringObject,
        )
    }

    private fun saveEachGatheringReceiptAndAttachPhoto(
        it: GatheringCreateCommand,
        gatheringObject: Gathering,
    ) {
        gatheringReceiptCommandService.saveReceiptDetails(it.receipt, gatheringObject)
    }

    private fun saveGatheringObject(
        bill: Bill,
        it: GatheringCreateCommand,
    ) = gatheringPersistencePort.save(bill, Gathering.create(it))

    override fun updateGatheringReceiptSplitAmount(receipt: GatheringReceipt): Boolean =
        gatheringReceiptCommandService.updateSplitAmount(receipt)

    override fun markAsCheckedEachGatheringMember(
        gatheringIds: List<GatheringId>,
        memberId: MemberId,
    ) {
        gatheringIds.forEach {
            val gatheringMembers = gatheringMemberQueryService.getGatheringMembersByGatheringIdAndMemberId(it, memberId)
            gatheringMemberCommandService.markAsChecked(gatheringMembers)
        }
    }
}
