package com.server.dpmcore.gathering.gathering.infrastructure.repository

import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringRepository(
    private val gatheringJpaRepository: GatheringJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : GatheringPersistencePort {
    override fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering = gatheringJpaRepository.save(GatheringEntity.from(bill, gathering)).toDomain()

    override fun findGatheringById(id: Long) =
        queryFactory
            .singleQuery<GatheringEntity> {
                select(entity(GatheringEntity::class))
                from(entity(GatheringEntity::class))
//                where(GatheringEntity::id id)
            }.toDomain()

    override fun findById(id: Long): Gathering =
        gatheringJpaRepository
            .findById(id)
            .orElseThrow { BillException.GatheringNotFoundException() }
            .toDomain()

    override fun saveAll(
        bill: Bill,
        gatherings: List<Gathering>,
    ) {
        gatheringJpaRepository
            .saveAll(
                gatherings.map { GatheringEntity.from(bill, it) },
            ).map { it.toDomain() }
    }

    override fun findByBillId(billId: Long): List<Gathering> =
        gatheringJpaRepository.findByBillId(billId).map {
            it.toDomain()
        }
}
