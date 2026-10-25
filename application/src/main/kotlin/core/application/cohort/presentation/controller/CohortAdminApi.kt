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
    @Operation(
        summary = "활성 기수 전환",
        description =
            "지정 기수를 활성화하고 이전 활성 기수의 활동 기록을 ENDED로 종료합니다. 정지 이력은 유지합니다. " +
                "현재 활성 기수 재요청은 종료하지 않습니다. 이전 기수 회원을 새 기수에 자동 승인하지 않으며 별도 승인이 필요합니다. " +
                "종료한 동일 기수의 활동은 복구할 수 없습니다.",
    )
    fun activateCohort(cohortId: CohortId): CustomResponse<CohortNumberResponse>
}
