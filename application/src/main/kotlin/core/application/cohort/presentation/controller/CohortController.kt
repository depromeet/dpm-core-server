package core.application.cohort.presentation.controller

import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortController(
    private val cohortQueryService: CohortQueryService
) : CohortApi {
    @GetMapping("/v1/cohort")
    override fun latestCohort(): CustomResponse<CohortNumberResponse> {
        val cohort = cohortQueryService.getLatestCohortValue()
        return CustomResponse.ok(CohortNumberResponse(cohort))
    }
}
