package com.server.dpmcore.bill.bill.infrastructure.repository

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.port.BillRepositoryPort
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
) : BillRepositoryPort {
    override fun save(bill: Bill): Bill {
        return billJpaRepository.save(BillEntity.from(bill)).toDomain()
    }
}
