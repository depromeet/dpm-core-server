package core.application.cohort.presentation.controller

import core.application.cohort.presentation.request.CohortUpsertRequest
import core.application.cohort.presentation.response.CohortListResponse
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.common.exception.CustomResponse
import core.domain.cohort.vo.CohortId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Cohort", description = "기수 API")
interface CohortApi {
    @ApiResponse(responseCode = "200", description = "현재 기수 조회 성공")
    @Operation(summary = "최신 기수 조회 API", description = "현재 활성화된 최신 기수를 조회합니다.")
    fun latestCohort(): CustomResponse<CohortNumberResponse>

    @ApiResponse(responseCode = "200", description = "기수 목록 조회 성공")
    @Operation(summary = "기수 목록 조회 API", description = "전체 기수 목록을 조회합니다.")
    fun getCohorts(): CustomResponse<CohortListResponse>

    @ApiResponse(responseCode = "200", description = "기수 생성 성공")
    @Operation(summary = "기수 생성 API", description = "새 기수를 생성하고 최신 기수 역할을 자동 생성합니다.")
    fun createCohort(request: CohortUpsertRequest): CustomResponse<CohortNumberResponse>

    @ApiResponse(responseCode = "200", description = "기수 수정 성공")
    @Operation(summary = "기수 수정 API", description = "기수 값을 수정합니다.")
    fun updateCohort(
        cohortId: CohortId,
        request: CohortUpsertRequest,
    ): CustomResponse<CohortNumberResponse>

    @ApiResponse(responseCode = "200", description = "기수 삭제 성공")
    @Operation(summary = "기수 삭제 API", description = "연관 데이터가 없는 기수를 삭제합니다.")
    fun deleteCohort(cohortId: CohortId): CustomResponse<Void>
}
