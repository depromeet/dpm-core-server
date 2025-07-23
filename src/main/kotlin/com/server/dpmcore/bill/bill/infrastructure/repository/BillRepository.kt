package com.server.dpmcore.bill.bill.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.outbound.BillPersistencePort
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import com.server.dpmcore.bill.exception.BillException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class BillRepository(
    private val billJpaRepository: BillJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : BillPersistencePort {
    override fun save(bill: Bill): BillId = BillId(billJpaRepository.save(BillEntity.from(bill)).id)

    override fun findById(billId: Long): Bill? =
        queryFactory
            .singleQuery<BillEntity> {
                select(entity(BillEntity::class))
                from(entity(BillEntity::class))
                where(col(BillEntity::id).equal(billId))
            }.toDomain()

    override fun findBillById(billId: Long): Bill =
        billJpaRepository.findByIdOrNull(billId)?.toDomain() ?: throw BillException.BillNotFoundException()
}
