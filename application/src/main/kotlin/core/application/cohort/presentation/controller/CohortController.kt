package core.application.cohort.presentation.controller

import core.application.cohort.application.service.CohortCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.request.CohortUpsertRequest
import core.application.cohort.presentation.response.CohortListResponse
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CohortController(
    private val cohortQueryService: CohortQueryService,
    private val cohortCommandService: CohortCommandService,
) : CohortApi {
    @PreAuthorize("permitAll()")
    @GetMapping("/v1/cohort", "/v1/cohorts/latest")
    override fun latestCohort(): CustomResponse<CohortNumberResponse> {
        val cohort = cohortQueryService.getLatestCohort()
        return CustomResponse.ok(cohort.toResponse())
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/v1/cohorts")
    override fun getCohorts(): CustomResponse<CohortListResponse> =
        CustomResponse.ok(
            CohortListResponse.from(cohortQueryService.getAllCohorts()),
        )

    @PreAuthorize("hasAuthority('create:cohort')")
    @PostMapping("/v1/cohorts")
    override fun createCohort(
        @Valid @RequestBody request: CohortUpsertRequest,
    ): CustomResponse<CohortNumberResponse> =
        CustomResponse.ok(
            cohortCommandService.create(request.value).toResponse(),
        )

    @PreAuthorize("hasAuthority('update:cohort')")
    @PutMapping("/v1/cohorts/{cohortId}")
    override fun updateCohort(
        @PathVariable cohortId: CohortId,
        @Valid @RequestBody request: CohortUpsertRequest,
    ): CustomResponse<CohortNumberResponse> =
        CustomResponse.ok(
            cohortCommandService.update(cohortId, request.value).toResponse(),
        )

    @PreAuthorize("hasAuthority('delete:cohort')")
    @DeleteMapping("/v1/cohorts/{cohortId}")
    override fun deleteCohort(
        @PathVariable cohortId: CohortId,
    ): CustomResponse<Void> {
        cohortCommandService.delete(cohortId)
        return CustomResponse.ok()
    }

    private fun Cohort.toResponse(): CohortNumberResponse =
        CohortNumberResponse(
            cohortId = this.id?.value ?: error("Cohort id must not be null"),
            cohortNumber = this.value,
        )
}
