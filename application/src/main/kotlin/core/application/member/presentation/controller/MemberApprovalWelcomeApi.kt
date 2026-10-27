package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberApprovalWelcomeApi {
    @Operation(
        summary = "본인 승인 완료 화면 확인",
        description =
            "승인 완료 화면에서 입장하기를 누르면 호출합니다. 요청 본문 없이 본인 토큰으로 확인하며 반복 호출해도 확인 완료를 유지합니다. " +
                "확인 후 GET /v3/members/me의 hasSeenApprovalWelcome이 true가 됩니다. 조회만으로는 확인 처리하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "확인 완료 또는 이미 확인함"),
            ApiResponse(responseCode = "401", description = "인증되지 않았거나 탈퇴한 회원"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 (MEMBER-404-01)"),
            ApiResponse(responseCode = "409", description = "승인 전 회원 (MEMBER-409-71)"),
        ],
    )
    fun confirm(
        @Parameter(hidden = true) memberId: Long,
    ): CustomResponse<Void>
}
