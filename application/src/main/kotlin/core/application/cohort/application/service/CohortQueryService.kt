package core.application.cohort.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.cohort.application.properties.CohortProperties
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CohortQueryService(
    private val cohortPersistencePort: CohortPersistencePort,
    private val cohortProperties: CohortProperties,
) : CohortQueryUseCase {
    fun getLatestCohort(): Cohort = getCohort(cohortProperties.value)

    override fun getLatestCohortId(): CohortId =
        getLatestCohort().id
            ?: throw CohortNotFoundException()

    override fun getLatestCohortValue(): String = getLatestCohort().value

    private fun getCohort(value: String): Cohort =
        cohortPersistencePort.findByValue(value)
            ?: throw CohortNotFoundException()
}
