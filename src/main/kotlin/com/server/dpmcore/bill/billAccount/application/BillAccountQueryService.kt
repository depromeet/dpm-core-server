package com.server.dpmcore.bill.billAccount.application

import com.server.dpmcore.bill.billAccount.application.exception.BillAccountNotFoundException
import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountPersistencePort
import org.springframework.stereotype.Service

@Service
class BillAccountQueryService(
    private val billAccountPersistencePort: BillAccountPersistencePort,
) {
    fun findBy(billAccountId: BillAccountId): BillAccount =
        billAccountPersistencePort.findById(
            billAccountId,
        ) ?: throw BillAccountNotFoundException()
}
