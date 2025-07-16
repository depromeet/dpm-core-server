package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.port.BillRepositoryPort
import com.server.dpmcore.bill.billAccount.application.BillAccountReadService
import com.server.dpmcore.gathering.gathering.application.GatheringCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BillCommandService(
    private val billRepositoryPort: BillRepositoryPort,
    private val gatheringCommandService: GatheringCommandService,
    private val billAccountReadService: BillAccountReadService,
) {
    fun save(bill: Bill): Bill {
//            TODO : 외에 다른 것들도 실드할 게 있으면 추가
        billAccountReadService.findBy(bill.billAccount).also {
            if (!it.equals(bill.billAccount)) throw IllegalArgumentException("존재하지 않는 회식 계좌입니다.")
        }
        if (bill.gatherings.isEmpty()) {
            throw IllegalArgumentException("회식은 필수로 존재해야합니다.")
        }

        val savedBill = billRepositoryPort.save(bill)
        bill.gatherings.forEach { gathering -> gathering.bill = savedBill }
        savedBill.gatherings = gatheringCommandService.save(bill.gatherings)
        return savedBill
    }
}
