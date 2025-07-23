package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.exception.BillException
import org.springframework.stereotype.Service

@Service
class BillQueryService(
    private val billPersistencePort: BillPersistencePort,
) : BillQueryUseCase {
    override fun findBillById(billId: BillId): Bill =
        billPersistencePort.findBillById(billId.value)
            ?: throw BillException.BillNotFoundException()
}
