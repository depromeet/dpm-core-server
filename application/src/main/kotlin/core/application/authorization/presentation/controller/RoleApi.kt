package core.application.authorization.presentation.controller

import core.application.authorization.presentation.response.RoleListResponse
import core.application.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Role", description = "역할 API")
interface RoleApi {
    @ApiResponse(responseCode = "200", description = "역할 목록 조회 성공")
    @Operation(
        summary = "역할 목록 조회 API",
        description =
            "모든 역할 목록을 조회 합니다. " +
                "정산서 생성 시 역할 별로 회식 인원을 초대하기 위해 사용 됩니다.",
    )
    fun getAllRoles(): CustomResponse<RoleListResponse>
}
