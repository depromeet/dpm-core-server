package core.application.cohort.presentation.request

import jakarta.validation.constraints.NotBlank

data class CohortUpsertRequest(
    @field:NotBlank
    val value: String,
)
