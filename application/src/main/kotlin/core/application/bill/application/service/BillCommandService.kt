package core.application.bill.application.service

import core.application.bill.application.exception.BillNotFoundException
import core.application.bill.application.service.account.BillAccountQueryService
import core.application.bill.application.validator.BillValidator
import core.application.bill.presentation.request.CreateBillRequest
import core.application.bill.presentation.request.UpdateGatheringJoinsRequest
import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.application.validator.GatheringReceiptValidator
import core.domain.bill.aggregate.Bill
import core.domain.bill.port.inbound.command.UpdateMemberDepositCommand
import core.domain.bill.port.inbound.command.UpdateMemberListDepositCommand
import core.domain.bill.port.outbound.BillPersistencePort
import core.domain.bill.vo.BillAccountId
import core.domain.bill.vo.BillId
import core.domain.gathering.port.inbound.GatheringCommandUseCase
import core.domain.gathering.port.inbound.GatheringQueryUseCase
import core.domain.gathering.port.inbound.command.GatheringCreateCommand
import core.domain.gathering.port.inbound.command.JoinGatheringCommand
import core.domain.gathering.port.inbound.command.ReceiptCommand
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billPersistencePort: BillPersistencePort,
    private val billAccountQueryService: BillAccountQueryService,
    private val gatheringCommandUseCase: GatheringCommandUseCase,
    private val gatheringQueryUseCase: GatheringQueryUseCase,
    private val billValidator: BillValidator,
    private val gatheringReceiptValidator: GatheringReceiptValidator,
) {
    fun saveBillWithGatherings(
        hostUserId: MemberId,
        request: CreateBillRequest,
    ): BillId {
        val billId = verifyAccountThenCreateBill(hostUserId, request)
        saveRelatedGatherings(request, billId, hostUserId)
        return billId
    }

    private fun saveRelatedGatherings(
        request: CreateBillRequest,
        billId: BillId,
        hostUserId: MemberId,
    ) {
        val gatheringCommands = request.gatherings.map { gathering ->
            GatheringCreateCommand(
                title = gathering.title,
                description = gathering.description,
                hostUserId = hostUserId,
                roundNumber = gathering.roundNumber,
                heldAt = gathering.heldAt,
                receipt = ReceiptCommand(
                    amount = gathering.receipt.amount
                )
            )
        }

        gatheringCommandUseCase.saveAllGatherings(gatheringCommands, request.invitedAuthorityIds, billId)
    }


    private fun verifyAccountThenCreateBill(
        hostUserId: MemberId,
        request: CreateBillRequest,
    ): BillId {
        val account = billAccountQueryService.findBy(BillAccountId(request.billAccountId))
        return billPersistencePort.save(
            Bill.create(
                account,
                request.title,
                request.description,
                hostUserId,
            ),
        )
    }

    fun closeBillParticipation(billId: BillId): BillId {
        val bill =
            billPersistencePort.findById(billId)
                ?: throw BillNotFoundException()

        billValidator.checkIsBillCompleted(bill)
        billValidator.checkIsBillClosable(bill)

        val gatherings = gatheringQueryUseCase.getAllGatheringsByBillId(billId)

        // GatheringReceipt 정산 금액 마감
        gatherings.forEach { gathering ->
            val gatheringId = gathering.id ?: throw GatheringNotFoundException()

            val joinMemberCount =
                gatheringQueryUseCase
                    .findGatheringMemberByGatheringId(gatheringId)
                    .count { it.isJoined == true }

            val receipt =
                gatheringQueryUseCase.findGatheringReceiptByGatheringId(gatheringId).run {
                    gatheringReceiptValidator.checkJoinMemberCountMoreThenOne(this, joinMemberCount)
                    gatheringReceiptValidator.checkIsExistsSplitAmount(this)
                    closeParticipation(joinMemberCount)
                }

            gatheringCommandUseCase.updateGatheringReceiptSplitAmount(receipt)
            // TODO: gathering.updatedAt 업데이트
        }
        billValidator.checkIsBillCompleted(bill)

        return billPersistencePort.save(bill.closeParticipation())
    }

    fun markBillAsChecked(
        billId: BillId,
        memberId: MemberId,
    ) {
        val gatheringIds = gatheringQueryUseCase.getAllGatheringIdsByBillId(billId)
        gatheringCommandUseCase.markAsCheckedEachGatheringMember(gatheringIds, memberId)
    }

    fun submitBillParticipationConfirm(
        billId: BillId,
        memberId: MemberId,
    ) = gatheringCommandUseCase.submitBillParticipationConfirmEachGathering(billId, memberId)

    fun markAsJoinedEachGathering(
        billId: BillId,
        request: UpdateGatheringJoinsRequest,
        memberId: MemberId,
    ) = gatheringCommandUseCase.markAsJoinedEachGatheringMember(
        billId = billId,
        command = request.gatheringJoins.map {
            JoinGatheringCommand(
                gatheringId = it.gatheringId,
                isJoined = it.isJoined,
            )
        },
        memberId = memberId,
    )


    fun updateMemberDeposit(command: UpdateMemberDepositCommand) {
        gatheringCommandUseCase.updateMemberDeposit(command)
    }

    fun updateMemberListDeposit(command: UpdateMemberListDepositCommand) {
        gatheringCommandUseCase.updateMemberListDeposit(command)
    }
}
