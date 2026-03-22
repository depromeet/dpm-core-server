package core.domain.cohort.port.outbound

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId

interface CohortPersistencePort {
    fun findAll(): List<Cohort>

    fun findById(cohortId: CohortId): Cohort?

    fun findByValue(value: String): Cohort?

    fun save(cohort: Cohort): Cohort

    fun deleteById(cohortId: CohortId)

    fun existsByValue(value: String): Boolean

    fun hasAnyReference(cohortId: CohortId): Boolean
}
