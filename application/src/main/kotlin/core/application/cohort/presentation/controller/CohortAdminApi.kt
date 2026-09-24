package core.application.cohort.presentation.controller

import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import core.domain.cohort.vo.CohortId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Cohort Admin", description = "활성 기수 관리 API")
interface CohortAdminApi {
    @ApiResponse(responseCode = "200", description = "활성 기수 조회 성공")
    @Operation(summary = "활성 기수 조회", description = "현재 is_active=true인 기수를 조회합니다.")
    fun getActiveCohort(): CustomResponse<CohortNumberResponse>

    @ApiResponse(responseCode = "200", description = "활성 기수 전환 성공")
    @Operation(summary = "활성 기수 전환", description = "지정 기수를 활성 기수로 전환합니다.")
    fun activateCohort(cohortId: CohortId): CustomResponse<CohortNumberResponse>
}
