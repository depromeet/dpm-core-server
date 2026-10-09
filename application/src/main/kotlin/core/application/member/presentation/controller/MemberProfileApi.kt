package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberProfileUpdateRequest
import core.application.member.presentation.response.MemberProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member Profile", description = "본인 이름과 파트 최초 입력")
interface MemberProfileApi {
    @Operation(summary = "본인 프로필 입력 상태 조회", description = "파트가 미배정이어도 최초 입력을 완료했다면 다시 입력을 요구하지 않습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공 (CustomResponse<MemberProfileResponse>)"),
            ApiResponse(responseCode = "401", description = "인증되지 않았거나 탈퇴한 회원"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 (MEMBER-404-01)"),
        ],
    )
    fun get(
        @Parameter(hidden = true) memberId: Long,
    ): CustomResponse<MemberProfileResponse>

    @Operation(
        summary = "본인 이름과 파트 최초 입력",
        description = "완료 후 동일 값은 유지하며 다른 값으로 변경하면 409를 반환합니다. 가입 대기와 승인 상태 모두 입력할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "입력 완료 또는 동일 값 유지 (CustomResponse<MemberProfileResponse>)"),
            ApiResponse(responseCode = "400", description = "필수 입력 누락 또는 잘못된 이름/파트 (MEMBER-400-41, MEMBER-400-05)"),
            ApiResponse(responseCode = "401", description = "인증되지 않았거나 탈퇴한 회원"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 (MEMBER-404-01)"),
            ApiResponse(responseCode = "409", description = "이미 완료한 프로필의 다른 값 변경 요청 (MEMBER-409-41)"),
        ],
    )
    fun complete(
        @Parameter(hidden = true) memberId: Long,
        request: MemberProfileUpdateRequest,
    ): CustomResponse<MemberProfileResponse>
}
