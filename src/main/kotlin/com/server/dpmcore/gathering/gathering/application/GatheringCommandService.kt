package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberDepositCommand
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositCommand
import com.server.dpmcore.bill.bill.presentation.dto.request.UpdateGatheringJoinsRequest
import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.exception.GatheringMemberException
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
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryByAuthorityUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringCommandService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val gatheringReceiptCommandService: GatheringReceiptCommandService,
    private val gatheringMemberCommandService: GatheringMemberCommandService,
    private val gatheringMemberQueryService: GatheringMemberQueryService,
    private val memberQueryByAuthorityUseCase: MemberQueryByAuthorityUseCase,
    private val billQueryUseCase: BillQueryUseCase,
) : GatheringCommandUseCase {
    override fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        invitedAuthorityIds: List<AuthorityId>,
        billId: BillId,
    ) {
        val bill = billQueryUseCase.getById(billId)
        val memberIds =
            memberQueryByAuthorityUseCase.findAllMemberIdByAuthorityIds(invitedAuthorityIds)
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
            val gatheringMember = gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(it, memberId)
            gatheringMemberCommandService.markAsChecked(gatheringMember)
        }
    }

    override fun markAsJoinedEachGatheringMember(
        billId: BillId,
        request: UpdateGatheringJoinsRequest,
        memberId: MemberId,
    ) {
        val gatheringIds = gatheringPersistencePort.findAllGatheringIdsByBillId(billId)
        val filteredGatheringIds = request.gatheringJoins.filter { it.gatheringId !in gatheringIds }
        if (filteredGatheringIds.isNotEmpty()) throw GatheringException.GatheringNotIncludedInBillException()

        request.gatheringJoins.forEach {
            val gatheringMember =
                gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(it.gatheringId, memberId)
            gatheringMemberCommandService.markAsJoined(gatheringMember, it.isJoined)
        }
    }

    override fun submitBillParticipationConfirmEachGathering(
        billId: BillId,
        memberId: MemberId,
    ) {
        val gatheringIds = gatheringPersistencePort.findAllGatheringIdsByBillId(billId)
        gatheringIds.map { gatheringId ->
            val gatheringMember =
                gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(
                    gatheringId,
                    memberId,
                )
            gatheringMemberCommandService.markAsGatheringParticipationSubmitConfirm(gatheringMember)
        }
    }

    override fun updateMemberDeposit(command: UpdateMemberDepositCommand) {
        billQueryUseCase.getById(command.billId)
        val gatherings = gatheringPersistencePort.findByBillId(command.billId)

        gatherings.forEach { gathering ->
            gathering.id ?: throw GatheringException.GatheringIdRequiredException()
            val gatheringMember =
                gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(gathering.id, command.memberId)

            gatheringMember.updateDeposit(command.isDeposit, command.memo)
            gatheringMemberCommandService.updateDeposit(gatheringMember)
        }
    }

    override fun updateMemberListDeposit(command: UpdateMemberListDepositCommand) {
        billQueryUseCase.getById(command.billId)
        val gatheringIds = gatheringPersistencePort.findAllGatheringIdsByBillId(command.billId)

        // 해당 멤버가 참여하는지 체크
        val gatheringMembers =
            gatheringMemberQueryService.getGatheringMemberByGatheringIdsAndMemberIds(
                gatheringIds,
                command.members.map {
                    it.memberId
                },
            )

        gatheringMembers.forEach { gatheringMember ->
            val isDeposit =
                command.members.find { member -> member.memberId == gatheringMember.memberId }?.isDeposit
                    ?: throw GatheringMemberException.GatheringMemberNotFoundException()
            gatheringMember.updateDeposit(isDeposit, null)
            gatheringMemberCommandService.updateDeposit(gatheringMember)
        }
    }
}
