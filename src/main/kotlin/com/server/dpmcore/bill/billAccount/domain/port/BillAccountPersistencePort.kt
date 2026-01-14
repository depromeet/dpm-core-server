package com.server.dpmcore.bill.billAccount.domain.port

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId

interface BillAccountPersistencePort {
    fun findById(billAccountId: BillAccountId): BillAccount?
}
