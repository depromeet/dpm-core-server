package core.domain.cohort.port.outbound

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.cohort.vo.CohortId

interface CohortPersistencePort {
    fun findAll(): List<Cohort>

    fun findById(cohortId: CohortId): Cohort?

    fun findByValue(value: String): Cohort?

    fun save(cohort: Cohort): Cohort

    fun deleteById(cohortId: CohortId)

    fun existsByValue(value: String): Boolean

    fun hasAnyReference(cohortId: CohortId): Boolean

    fun findActive(): Cohort?

    fun deactivateAll()

    fun activate(cohortId: CohortId)

    /** 기수에 만들어진 모든 팀. 팀 번호, ID 오름차순 */
    fun findTeamsByCohortId(cohortId: CohortId): List<CohortTeamQueryModel>
}
