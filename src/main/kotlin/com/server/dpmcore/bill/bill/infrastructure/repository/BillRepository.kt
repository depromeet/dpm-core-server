package com.server.dpmcore.bill.bill.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import com.server.dpmcore.bill.exception.BillException
import org.jooq.DSLContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
) : BillPersistencePort {
    override fun save(bill: Bill): BillId = BillId(billJpaRepository.save(BillEntity.from(bill)).id)

    override fun findById(billId: BillId): Bill =
        billJpaRepository.findByIdOrNull(billId.value)?.toDomain() ?: throw BillException.BillNotFoundException()

    override fun findAllBills(): List<Bill> = billJpaRepository.findAll().map { it.toDomain() }

    override fun closeBillParticipation(bill: Bill): Int =
        queryFactory
            .updateQuery(BillEntity::class) {
                set(col(BillEntity::billStatus), bill.billStatus.name)
                set(col(BillEntity::updatedAt), bill.updatedAt)
                where(col(BillEntity::id).equal(bill.id?.value ?: throw BillException.BillIdRequiredException()))
            }.executeUpdate()
}
