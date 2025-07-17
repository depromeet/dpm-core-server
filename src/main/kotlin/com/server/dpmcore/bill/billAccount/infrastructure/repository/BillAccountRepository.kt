package com.server.dpmcore.bill.billAccount.infrastructure.repository

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountPersistencePort
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class BillAccountRepository(
    private val billAccountJpaRepository: BillAccountJpaRepository,
) : BillAccountPersistencePort {
    override fun findById(id: Long): BillAccount? = billAccountJpaRepository.findById(id).getOrNull()?.toDomain()
}
