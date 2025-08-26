package com.server.dpmcore.cohort.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.cohort.domain.port.outbound.CohortPersistencePort
import com.server.dpmcore.cohort.infrastructure.entity.CohortEntity
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import org.springframework.stereotype.Repository

@Repository
class CohortRepository(
    private val cohortJpaRepository: CohortJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : CohortPersistencePort {
    override fun findCohortIdByValue(value: String): CohortId? {
        return queryFactory
            .singleQueryOrNull<Long> {
                select(col(CohortEntity::id))
                from(entity(CohortEntity::class))
                where(col(CohortEntity::value).equal(value))
            }?.let { return CohortId(it) }
    }
}
