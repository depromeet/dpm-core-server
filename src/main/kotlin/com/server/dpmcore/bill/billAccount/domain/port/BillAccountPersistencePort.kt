package com.server.dpmcore.bill.billAccount.domain.port

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount

interface BillAccountPersistencePort {
    fun findById(id: Long): BillAccount?
}
