package com.server.dpmcore.bill.billAccount.application

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountPersistencePort
import com.server.dpmcore.bill.exception.BillAccountException
import org.springframework.stereotype.Service

@Service
class BillAccountQueryService(
    private val billAccountPersistencePort: BillAccountPersistencePort,
) {
    fun findBy(billAccountId: BillAccountId): BillAccount {
        println("Finding BillAccount with ID: $billAccountId")
        return billAccountPersistencePort.findById(
            billAccountId,
        ) ?: throw BillAccountException.BillAccountNotFoundException()
    }
}
