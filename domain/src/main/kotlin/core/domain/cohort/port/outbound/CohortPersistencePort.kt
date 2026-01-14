package core.domain.cohort.port.outbound

import core.domain.cohort.aggregate.Cohort

interface CohortPersistencePort {
    fun findByValue(value: String): Cohort?
}
