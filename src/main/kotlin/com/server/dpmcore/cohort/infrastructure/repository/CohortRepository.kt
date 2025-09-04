package com.server.dpmcore.cohort.infrastructure.repository

import com.server.dpmcore.cohort.domain.model.Cohort
import com.server.dpmcore.cohort.domain.port.outbound.CohortPersistencePort
import org.springframework.stereotype.Repository

@Repository
class CohortRepository(
    private val cohortJpaRepository: CohortJpaRepository,
) : CohortPersistencePort {
    override fun findByValue(value: String): Cohort {
        return cohortJpaRepository.findByValue(value).toDomain()
    }
}
