package core.persistence.bill.repository

import core.domain.bill.aggregate.Bill
import core.domain.bill.port.outbound.BillPersistencePort
import core.domain.bill.vo.BillId
import core.entity.bill.BillEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
) : BillPersistencePort {
    override fun save(bill: Bill): BillId = BillId(billJpaRepository.save(BillEntity.from(bill)).id)

    override fun findById(billId: BillId): Bill? = billJpaRepository.findByIdOrNull(billId.value)?.toDomain()

    override fun findAllBills(): List<Bill> = billJpaRepository.findAll().map { it.toDomain() }
}
