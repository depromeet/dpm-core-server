package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberProfileUpdateRequest
import core.application.member.presentation.response.MemberProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member Profile", description = "본인 이름과 파트 입력·수정")
interface MemberProfileApi {
    @Operation(
        summary = "본인 프로필 입력 상태 조회",
        description =
            "파트가 미배정이어도 최초 입력을 완료했다면 다시 입력을 요구하지 않습니다. " +
                "가입이 반려되면 기존 값을 보존한 채 재입력을 요구합니다. " +
                "profileEditable은 수정 가능 여부이고 profileCompletionRequired는 필수 입력 여부입니다.",
    )
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
        summary = "본인 이름과 파트 입력·수정",
        description =
            "가입 대기(PENDING)는 제출 후에도 이름·파트를 수정할 수 있습니다. 수정해도 가입·재신청 시각은 바뀌지 않습니다. " +
                "승인 후 최초 보완은 허용하지만 완료된 값의 변경은 409를 반환합니다. 동일 값 재전송은 유지합니다. " +
                "반려 후에는 이름·파트를 다시 입력한 뒤 별도 재신청 API를 호출해야 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "입력 완료 또는 동일 값 유지 (CustomResponse<MemberProfileResponse>)"),
            ApiResponse(responseCode = "400", description = "필수 입력 누락 또는 잘못된 이름/파트 (MEMBER-400-41, MEMBER-400-05)"),
            ApiResponse(responseCode = "401", description = "인증되지 않았거나 탈퇴한 회원"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 (MEMBER-404-01)"),
            ApiResponse(responseCode = "409", description = "가입 대기가 아닌 회원의 완료된 프로필 변경 요청 (MEMBER-409-41)"),
        ],
    )
    fun complete(
        @Parameter(hidden = true) memberId: Long,
        request: MemberProfileUpdateRequest,
    ): CustomResponse<MemberProfileResponse>
}
