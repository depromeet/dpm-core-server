package com.server.dpmcore.bill.bill.infrastructure.repository

import com.server.dpmcore.bill.bill.application.exception.BillNotFoundException
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
) : BillPersistencePort {
    override fun save(bill: Bill): BillId = BillId(billJpaRepository.save(BillEntity.from(bill)).id)

    override fun findById(billId: BillId): Bill =
        billJpaRepository.findByIdOrNull(billId.value)?.toDomain() ?: throw BillNotFoundException()

    override fun findAllBills(): List<Bill> = billJpaRepository.findAll().map { it.toDomain() }
}
