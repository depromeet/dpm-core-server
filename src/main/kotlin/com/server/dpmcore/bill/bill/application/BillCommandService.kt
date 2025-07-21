package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.port.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.presentation.mapper.BillMapper.toBill
import com.server.dpmcore.bill.billAccount.application.BillAccountQueryService
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.application.GatheringCommandService
import com.server.dpmcore.gathering.gathering.presentation.mapper.GatheringMapper.toGathering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billPersistencePort: BillPersistencePort,
    private val gatheringCommandService: GatheringCommandService,
    private val billAccountQueryService: BillAccountQueryService,
) {
    fun save(createBillRequest: CreateBillRequest): Bill {
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
    }
}
