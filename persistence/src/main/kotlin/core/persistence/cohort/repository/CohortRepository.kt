package core.persistence.cohort.repository

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import org.springframework.stereotype.Repository

@Repository
class CohortRepository(
    private val cohortJpaRepository: CohortJpaRepository,
) : CohortPersistencePort {
    override fun findByValue(value: String): Cohort {
        return cohortJpaRepository.findByValue(value).toDomain()
    }
}
