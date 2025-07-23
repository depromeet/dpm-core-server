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
import com.server.dpmcore.gathering.gatheringReceipt.application.ReceiptCommandService
import com.server.dpmcore.gathering.gatheringReceipt.application.ReceiptQueryService
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billPersistencePort: BillPersistencePort,
    private val billAccountQueryService: BillAccountQueryService,
    private val gatheringCreateUseCase: GatheringCreateUseCase,
    private val gatheringReceiptQueryService: ReceiptQueryService,
    private val gatheringReceiptCommandService: ReceiptCommandService,
    private val gatheringQueryService: GatheringQueryService,
) {
    fun saveBillWithGatherings(request: CreateBillRequest): BillId {
        val billId = verifyAccountThenCreateBill(request)
        saveRelatedGatherings(request, billId)
        return billId
    }

    private fun saveRelatedGatherings(
        request: CreateBillRequest,
        billId: BillId,
    ) {
        val gatheringCommands = request.gatherings.map { it.toCommand() }
        gatheringCreateUseCase.saveAllGatherings(gatheringCommands, billId)
    }

    private fun verifyAccountThenCreateBill(request: CreateBillRequest): BillId {
        val account = billAccountQueryService.findBy(request.billAccountId)
        return billPersistencePort.save(
            Bill.create(
                account,
                request.title,
                request.description,
                MemberId(request.hostUserId),
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

        // GatheringReceipt 정산 금액 마감
        gatherings.map { gathering ->
            gathering.id ?: throw BillException.GatheringNotFoundException()
            val receipt =
                gatheringReceiptQueryService
                    .findBy(
                        gathering.id.value,
                    ).closeParticipation(gathering.getGatheringJoinMemberCount())
            gatheringReceiptCommandService.save(receipt)
        }
        bill.complete()
        billPersistencePort.save(bill)
    }

    /*
    fun save(createBillRequest: CreateBillRequest): Bill {
        try {
 //            TODO : 외에 다른 것들도 실드할 게 있으면 추가
            billAccountQueryService.findBy(createBillRequest.billAccountId).also {
                if (it.id?.value != createBillRequest.billAccountId) throw BillException.BillAccountNotFoundException()
            }
            if (createBillRequest.gatherings.isEmpty()) {
                throw BillException.GatheringRequiredException()
            }
            val bill = toBill(createBillRequest)
            val savedBill = billPersistencePort.save(bill)
 //        TODO : 값 주입 도메인 로직으로 변경

            gatheringCommandService.save(
                bill = savedBill,
                createBillRequest.gatherings
                    .map { createGatheringRequest ->
                        toGathering(
                            createGatheringRequest,
                            savedBill.id ?: throw BillException.BillIdRequiredException(),
                        )
                    }.toMutableList(),
            )
            return savedBill
        } catch (e: Exception) {
            e.printStackTrace()
        }
        throw BillException.BillIdRequiredException()
    }

     */
}
