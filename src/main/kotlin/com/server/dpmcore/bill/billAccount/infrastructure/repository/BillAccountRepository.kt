package com.server.dpmcore.bill.billAccount.infrastructure.repository

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class BillAccountRepository(
    private val billAccountJpaRepository: BillAccountJpaRepository,
) : BillAccountRepositoryPort {
    override fun findById(id: Long): BillAccount {
        return billAccountJpaRepository.findById(id).orElseThrow {
            throw IllegalArgumentException("BillAccount $id 는 존재하지 않는 값입니다. ")
        }.toDomain()
    }
}
