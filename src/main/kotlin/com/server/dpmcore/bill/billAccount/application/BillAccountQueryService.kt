package com.server.dpmcore.bill.billAccount.application

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountPersistencePort
import com.server.dpmcore.bill.exception.BillException
import org.springframework.stereotype.Service

@Service
class BillAccountQueryService(
    private val billAccountPersistencePort: BillAccountPersistencePort,
) {
    fun findBy(billAccountId: Long): BillAccount =
        billAccountPersistencePort.findById(
            billAccountId,
        ) ?: throw BillException.BillAccountNotFoundException()
}
