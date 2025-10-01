package core.persistence.cohort.repository

import core.entity.cohort.CohortEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CohortJpaRepository : JpaRepository<CohortEntity, Long> {
    fun findByValue(value: String): CohortEntity
}
