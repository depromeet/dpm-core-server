package com.server.dpmcore.bill.bill.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId

interface BillQueryUseCase {
    fun getById(billId: BillId): Bill
}
