package core.application.cohort.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.cohort.application.properties.CohortProperties
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
    override fun getLatestCohortId(): CohortId =
        getCohort(cohortProperties.value).id
            ?: throw CohortNotFoundException()

    fun getLatestCohortValue(): String =
        getCohort(cohortProperties.value).value

    private fun getCohort(value: String) = cohortPersistencePort.findByValue(value)
        ?: throw CohortNotFoundException()

}
