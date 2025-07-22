package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.response.CreateBillResponse
import com.server.dpmcore.bill.bill.presentation.mapper.BillMapper
import com.server.dpmcore.bill.exception.BillException
import org.springframework.stereotype.Service

@Service
class BillQueryService(
    private val billPersistencePort: BillPersistencePort,
    private val billMapper: BillMapper,
) : BillQueryUseCase {
    override fun getById(billId: BillId): Bill =
        billPersistencePort.findById(billId.value)
            ?: throw BillException.BillNotFoundException()

    fun getBillDetails(billId: BillId): CreateBillResponse = billMapper.toBillCreateResponse(getById(billId))
}
