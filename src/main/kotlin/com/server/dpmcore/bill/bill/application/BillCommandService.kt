package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.application.mapper.BillGatheringMapper.toCommand
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.model.BillStatus
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.billAccount.application.BillAccountQueryService
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.application.GatheringQueryService
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringCreateUseCase
import com.server.dpmcore.gathering.gatheringReceipt.application.GatheringReceiptCommandService
import com.server.dpmcore.gathering.gatheringReceipt.application.GatheringReceiptQueryService
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billPersistencePort: BillPersistencePort,
    private val billAccountQueryService: BillAccountQueryService,
    private val gatheringCreateUseCase: GatheringCreateUseCase,
    private val gatheringReceiptQueryService: GatheringReceiptQueryService,
    private val gatheringReceiptCommandService: GatheringReceiptCommandService,
    private val gatheringQueryService: GatheringQueryService,
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
        gatheringCreateUseCase.saveAllGatherings(gatheringCommands, request.invitedAuthorityIds, billId)
    }

    private fun verifyAccountThenCreateBill(
        hostUserId: MemberId,
        request: CreateBillRequest,
    ): BillId {
        val account = billAccountQueryService.findBy(request.billAccountId)
        return billPersistencePort.save(
            Bill.create(
                account,
                request.title,
                request.description,
                hostUserId,
            ),
        )
    }

    fun closeBillParticipation(billId: Long) {
        val bill =
            billPersistencePort.findBillById(billId)
                ?: throw BillException.BillNotFoundException()
        if (bill.billStatus != BillStatus.OPEN) {
            throw BillException.BillCannotCloseParticipationException()
        }

        val gatherings = gatheringQueryService.findByBillId(billId)

        println("111111")
        // GatheringReceipt 정산 금액 마감
        gatherings.map { gathering ->
            gathering.id ?: throw BillException.GatheringNotFoundException()
            val receipt =
                gatheringReceiptQueryService
                    .findBy(
                        gathering.id.value,
                    ).closeParticipation(gathering.getGatheringJoinMemberCount())
            gatheringReceiptCommandService.updateSplitAmount(receipt, gathering)
        }

        println("22222")
        val closeParticipationBill = bill.closeParticipation()
        println("closeParticipationBill: $closeParticipationBill")
        billPersistencePort.save(closeParticipationBill)
    }
}
