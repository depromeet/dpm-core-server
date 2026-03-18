package core.application.cohort.presentation.controller

import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortController(
    private val cohortQueryService: CohortQueryService,
) : CohortApi {
    @PreAuthorize("permitAll()")
    @GetMapping("/v1/cohort")
    override fun latestCohort(): CustomResponse<CohortNumberResponse> {
        val cohort = cohortQueryService.getLatestCohort()
        return CustomResponse.ok(
            CohortNumberResponse(
                cohortId = cohort.id?.value ?: error("Latest cohort id must not be null"),
                cohortNumber = cohort.value,
            ),
        )
    }
}
