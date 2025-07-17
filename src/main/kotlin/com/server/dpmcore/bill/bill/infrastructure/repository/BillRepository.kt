package com.server.dpmcore.bill.bill.infrastructure.repository

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.port.BillPersistencePort
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
) : BillPersistencePort {
    override fun save(bill: Bill): Bill = billJpaRepository.save(BillEntity.from(bill)).toDomain()
}
