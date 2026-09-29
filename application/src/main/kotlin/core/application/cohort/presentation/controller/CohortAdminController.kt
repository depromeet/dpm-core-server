package core.application.cohort.presentation.controller

import core.application.cohort.application.service.CohortCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortAdminController(
    private val cohortQueryService: CohortQueryService,
    private val cohortCommandService: CohortCommandService,
) : CohortAdminApi {
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/v1/cohorts/active")
    override fun getActiveCohort(): CustomResponse<CohortNumberResponse> =
        CustomResponse.ok(cohortQueryService.getActiveCohort().toResponse())

    @PreAuthorize("hasAuthority('update:cohort')")
    @PatchMapping("/v1/cohorts/{cohortId}/activate")
    override fun activateCohort(
        @PathVariable cohortId: CohortId,
    ): CustomResponse<CohortNumberResponse> =
        CustomResponse.ok(cohortCommandService.activateCohort(cohortId).toResponse())

    private fun Cohort.toResponse(): CohortNumberResponse =
        CohortNumberResponse(
            cohortId = this.id?.value ?: error("Cohort id must not be null"),
            cohortNumber = this.value,
        )
}
