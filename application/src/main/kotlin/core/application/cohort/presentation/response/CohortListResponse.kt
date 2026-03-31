package core.application.cohort.presentation.response

import core.domain.cohort.aggregate.Cohort

data class CohortListResponse(
    val cohorts: List<CohortNumberResponse>,
) {
    companion object {
        fun from(cohorts: List<Cohort>): CohortListResponse =
            CohortListResponse(
                cohorts =
                    cohorts.map { cohort ->
                        CohortNumberResponse(
                            cohortId = cohort.id?.value ?: error("Cohort id must not be null"),
                            cohortNumber = cohort.value,
                        )
                    },
            )
    }
}
