package core.persistence.cohort.repository

import core.persistence.cohort.entity.CohortEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CohortJpaRepository : JpaRepository<CohortEntity, Long> {
    fun findByValue(value: String): CohortEntity
}
