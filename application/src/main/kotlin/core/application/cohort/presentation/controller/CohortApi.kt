package core.application.cohort.presentation.controller

import core.application.cohort.presentation.request.CohortUpsertRequest
import core.application.cohort.presentation.response.CohortListResponse
import core.application.cohort.presentation.response.CohortNumberResponse
import core.application.cohort.presentation.response.CohortTeamsResponse
import core.application.common.exception.CustomResponse
import core.domain.cohort.vo.CohortId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
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

    @ApiResponse(
        responseCode = "200",
        description = "현재 기수 팀 목록 조회 성공",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "현재 기수 팀 목록 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "message": "요청에 성공했습니다",
                                "code": "GLOBAL-200-01",
                                "data": {
                                    "teams": [
                                        { "id": 31, "number": 1 },
                                        { "id": 32, "number": 2 },
                                        { "id": 37, "number": 7 }
                                    ]
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "현재 기수 팀 목록 조회 API (운영진)",
        description =
            "현재 활성 기수에 만들어진 모든 팀을 팀 번호, 팀 ID 오름차순으로 조회합니다. 파라미터는 없습니다. " +
                "멤버가 배정되지 않은 팀도 포함하며 출석 명단과 무관합니다.",
    )
    fun getCurrentCohortTeams(): CustomResponse<CohortTeamsResponse>

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
