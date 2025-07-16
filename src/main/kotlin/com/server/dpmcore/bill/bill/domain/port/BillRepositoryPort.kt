package com.server.dpmcore.bill.bill.domain.port

import com.server.dpmcore.bill.bill.domain.model.Bill

interface BillRepositoryPort {
    fun save(bill: Bill): Bill
}
