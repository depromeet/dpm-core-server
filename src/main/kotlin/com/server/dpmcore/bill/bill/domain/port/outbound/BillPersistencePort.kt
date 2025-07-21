package com.server.dpmcore.bill.bill.domain.port.outbound

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId

interface BillPersistencePort {
    fun save(bill: Bill): BillId

    fun findById(billId: Long): Bill?
}
