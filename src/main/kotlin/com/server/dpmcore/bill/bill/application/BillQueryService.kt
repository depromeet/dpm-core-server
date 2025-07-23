package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.bill.presentation.mapper.BillMapper
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service

@Service
class BillQueryService(
    private val billPersistencePort: BillPersistencePort,
    private val billMapper: BillMapper,
) : BillQueryUseCase {
    override fun getById(billId: BillId): Bill =
        billPersistencePort.findBillById(billId.value)
            ?: throw BillException.BillNotFoundException()

    fun getBillDetails(billId: BillId): BillDetailResponse = billMapper.toBillDetailResponse(getById(billId))

    override fun getBillByMemberId(memberId: MemberId): BillListResponse = getAllBills()

    fun getAllBills(): BillListResponse {
        val bills = billPersistencePort.findAllBills()
        return billMapper.toBillListResponse(bills)
    }
}
