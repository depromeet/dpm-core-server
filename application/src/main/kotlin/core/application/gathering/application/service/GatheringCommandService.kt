package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringIdRequiredException
import core.application.gathering.application.exception.GatheringNotIncludedInBillException
import core.application.gathering.application.exception.member.GatheringMemberNotFoundException
import core.application.gathering.application.service.member.GatheringMemberCommandService
import core.application.gathering.application.service.member.GatheringMemberQueryService
import core.application.gathering.application.service.receipt.GatheringReceiptCommandService
import core.domain.authority.vo.AuthorityId
import core.domain.bill.aggregate.Bill
import core.domain.bill.port.inbound.BillQueryUseCase
import core.domain.bill.port.inbound.command.UpdateMemberDepositCommand
import core.domain.bill.port.inbound.command.UpdateMemberListDepositCommand
import core.domain.bill.vo.BillId
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.inbound.GatheringCommandUseCase
import core.domain.gathering.port.inbound.command.GatheringCreateCommand
import core.domain.gathering.port.inbound.command.JoinGatheringCommand
import core.domain.gathering.port.outbound.GatheringPersistencePort
import core.domain.gathering.vo.GatheringId
import core.domain.member.port.inbound.MemberQueryByAuthorityUseCase
import core.domain.member.vo.MemberId
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
        commands: List<JoinGatheringCommand>,
        memberId: MemberId,
    ) {
        val gatheringIds = gatheringPersistencePort.findAllGatheringIdsByBillId(billId)

        val invalidGatheringIds = commands
            .map { it.gatheringId }
            .filterNot { it in gatheringIds }

        if (invalidGatheringIds.isNotEmpty()) {
            throw GatheringNotIncludedInBillException()
        }

        commands.forEach { cmd ->
            val gatheringMember =
                gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(cmd.gatheringId, memberId)

            if (cmd.isJoined) {
                gatheringMemberCommandService.markAsJoined(gatheringMember)
            }
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
            val gatheringMember =
                gatheringMemberQueryService.getGatheringMemberByGatheringIdAndMemberId(
                    gathering.id ?: throw GatheringIdRequiredException(), command.memberId,
                )
            gatheringMemberCommandService.updateDepositAndMemo(gatheringMember, command.isDeposit, command.memo)
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
                    ?: throw GatheringMemberNotFoundException()
            gatheringMemberCommandService.updateDepositAndMemo(gatheringMember, isDeposit, null)
        }
    }
}
