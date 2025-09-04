package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.application.exception.BillNotFoundException
import com.server.dpmcore.bill.bill.application.mapper.BillGatheringMapper.toCommand
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberDepositCommand
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositCommand
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.presentation.dto.request.UpdateGatheringJoinsRequest
import com.server.dpmcore.bill.billAccount.application.BillAccountQueryService
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotFoundException
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringCommandUseCase
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billPersistencePort: BillPersistencePort,
    private val billAccountQueryService: BillAccountQueryService,
    private val gatheringCommandUseCase: GatheringCommandUseCase,
    private val gatheringQueryUseCase: GatheringQueryUseCase,
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
        val gatheringCommands = request.gatherings.map { it.toCommand(hostUserId) }
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

        bill.checkParticipationClosable()

        val gatherings = gatheringQueryUseCase.getAllGatheringsByBillId(billId)

        // GatheringReceipt 정산 금액 마감
        gatherings.map { gathering ->
            gathering.id ?: throw GatheringNotFoundException()

            val gatheringMember = gatheringQueryUseCase.findGatheringMemberByGatheringId(gathering.id)

            val receipt =
                gatheringQueryUseCase
                    .findGatheringReceiptByGatheringId(
                        gathering.id,
                    ).closeParticipation(
                        gatheringMember.count {
                            it.isJoined == true
                        },
                    )
            gatheringCommandUseCase.updateGatheringReceiptSplitAmount(receipt)
//            TODO : gathering updatedAt 업데이트
        }

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
    ) = gatheringCommandUseCase.markAsJoinedEachGatheringMember(billId, request, memberId)

    fun updateMemberDeposit(command: UpdateMemberDepositCommand) {
        gatheringCommandUseCase.updateMemberDeposit(command)
    }

    fun updateMemberListDeposit(command: UpdateMemberListDepositCommand) {
        gatheringCommandUseCase.updateMemberListDeposit(command)
    }
}
