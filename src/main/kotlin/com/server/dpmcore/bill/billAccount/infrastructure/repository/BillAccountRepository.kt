package com.server.dpmcore.bill.billAccount.infrastructure.repository

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountPersistencePort
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class BillAccountRepository(
    private val billAccountJpaRepository: BillAccountJpaRepository,
) : BillAccountPersistencePort {
    override fun findById(billAccountId: BillAccountId): BillAccount? {
        val billAccount = billAccountJpaRepository.findById(billAccountId.value).getOrNull()
        println("Finding BillAccount with ID: $billAccountId, Result: $billAccount")
        return billAccount?.toDomain()
    }
}
