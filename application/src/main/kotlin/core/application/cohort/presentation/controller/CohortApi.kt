package core.application.cohort.presentation.controller

import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Cohort", description = "기수 API")
interface CohortApi {
    @ApiResponse(responseCode = "200", description = "현재 기수 조회 성공")
    @Operation(summary = "현재 기수 조회 API", description = "현재 활성화된 기수를 조회 합니다.")
    fun latestCohort(): CustomResponse<CohortNumberResponse>
}
