package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberMergeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberAdmissionApi {
    @Operation(
        summary = "미승인 계정 통합 및 승인",
        description =
            "create:member, delete:member 권한이 필요합니다. 서로 다른 두 계정 모두 현재 기수 또는 기수 없는 PENDING이어야 합니다. " +
                "유지할 계정의 닉네임·파트를 보존하고 원본의 로그인 수단을 옮긴 뒤 현재 기수의 디퍼, 팀 미배정으로 승인합니다. " +
                "관리 목록 이메일은 카카오를 우선합니다. 다른 회원에게 속한 로그인 수단이나 동일 provider의 서로 다른 계정은 덮어쓰지 않습니다. " +
                "통합·원본 정리·승인을 한 트랜잭션으로 처리하며 실패하면 전체 취소합니다. 이미 처리된 계정의 재요청은 거절합니다. " +
                "비밀번호 로그인 정보가 있는 계정은 통합할 수 없습니다. 원본 회원은 soft delete하며 연관 기록을 보존합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "통합 및 승인 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 요청 또는 미승인 대상 조건 불충족"),
        ApiResponse(responseCode = "403", description = "통합·승인 권한 없음"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
        ApiResponse(responseCode = "409", description = "로그인 수단 충돌"),
    )
    fun mergeAndApprove(request: MemberMergeRequest): CustomResponse<Void>

    @Operation(
        summary = "가입 반려",
        description =
            "create:member 권한으로 현재 기수 또는 기수 없는 PENDING 멤버를 반려합니다. 기록을 보존하고 미승인 목록·집계에서 제외합니다. " +
                "이미 반려되거나 승인된 멤버는 거절합니다. 반려 사유 입력은 받지 않습니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "반려 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 ID 또는 반려할 수 없는 대상"),
        ApiResponse(responseCode = "403", description = "가입 관리 권한 없음"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
    )
    fun reject(memberId: Long): CustomResponse<Void>

    @Operation(
        summary = "본인 가입 재신청",
        description =
            "로그인한 반려 회원 본인만 명시적으로 재신청합니다. 과거 소속과 반려 이력을 보존하고 현재 기수에 PENDING으로 재신청합니다. " +
                "단순 재로그인으로 재신청하지 않습니다. 이미 PENDING인 반복 요청은 추가 신청 기록 없이 성공하며 승인 회원은 거절합니다. " +
                "회원 ID는 토큰에서 가져옵니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "가입 대기 전환 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "재신청할 수 없는 회원 상태"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "접근 거절"),
        ApiResponse(responseCode = "404", description = "멤버 또는 신청할 기수 없음"),
    )
    fun reapply(
        @Parameter(hidden = true) memberId: Long,
    ): CustomResponse<Void>
}
