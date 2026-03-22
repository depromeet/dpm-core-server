package core.application.cohort.application.service

import core.application.cohort.application.exception.CohortNotFoundException
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
) : CohortQueryUseCase {
    fun getLatestCohort(): Cohort =
        cohortPersistencePort
            .findAll()
            .maxByOrNull { toNumericCohortValue(it.value) ?: Int.MIN_VALUE }
            ?.takeIf { isNumericCohortValue(it.value) }
            ?: throw CohortNotFoundException()

    override fun getLatestCohortId(): CohortId =
        getLatestCohort().id
            ?: throw CohortNotFoundException()

    override fun getLatestCohortValue(): String = getLatestCohort().value

    fun getAllCohorts(): List<Cohort> =
        cohortPersistencePort
            .findAll()
            .sortedWith(
                compareByDescending<Cohort> { toNumericCohortValue(it.value) ?: Int.MIN_VALUE }
                    .thenByDescending { it.createdAt ?: 0L },
            )

    fun getCohort(cohortId: CohortId): Cohort =
        cohortPersistencePort.findById(cohortId)
            ?: throw CohortNotFoundException()

    fun getPreviousNumericCohortValue(targetValue: String): String? {
        val target = toNumericCohortValue(targetValue) ?: return null
        return cohortPersistencePort
            .findAll()
            .mapNotNull { cohort -> toNumericCohortValue(cohort.value)?.let { it to cohort.value } }
            .filter { (numericValue, _) -> numericValue < target }
            .maxByOrNull { (numericValue, _) -> numericValue }
            ?.second
    }

    fun isNumericCohortValue(value: String): Boolean = NUMERIC_COHORT_REGEX.matches(value.trim())

    private fun toNumericCohortValue(value: String): Int? =
        value
            .trim()
            .takeIf { NUMERIC_COHORT_REGEX.matches(it) }
            ?.toIntOrNull()

    companion object {
        private val NUMERIC_COHORT_REGEX = Regex("\\d+")
    }
}
