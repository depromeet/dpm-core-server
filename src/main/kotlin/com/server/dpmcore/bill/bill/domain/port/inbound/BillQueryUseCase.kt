package com.server.dpmcore.bill.bill.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.member.member.domain.model.MemberId

interface BillQueryUseCase {
    fun getById(billId: BillId): Bill

    fun getBillByMemberId(memberId: MemberId): BillListResponse
}
