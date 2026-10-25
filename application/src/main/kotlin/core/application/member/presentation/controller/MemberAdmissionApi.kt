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
        summary = "가입 대기 계정 병합 및 승인",
        description =
            "create:member, delete:member 권한이 필요합니다. sourceMemberId는 현재 기수 또는 기수 없는 PENDING이어야 합니다. " +
                "retainedMemberId는 같은 조건의 PENDING 또는 현재 기수 ACTIVE를 지정합니다. PENDING은 이름·파트를 보존하고 " +
                "현재 기수 디퍼·팀 미배정으로 승인하며, ACTIVE는 기존 이름·파트·기수·역할·팀·활동 기록을 그대로 유지합니다. " +
                "원본의 소셜 로그인 연결을 옮기고 원본은 soft delete합니다. 원본의 refresh 세션과 임시 연동 요청을 정리합니다. " +
                "같은 provider의 서로 다른 계정과 비밀번호 로그인 정보가 있는 계정은 통합할 수 없습니다. " +
                "처리자·대상·시점을 기록하며 모든 변경을 한 트랜잭션으로 처리합니다. 실패하면 전체 취소하고 이미 처리된 대상은 거절합니다. " +
                "중복 의심은 병합을 강제하지 않습니다. 다른 사람이라면 일반 승인 API, 승인 불가라면 반려 API를 사용합니다. " +
                "확인 화면의 후보는 GET /v3/members/overview에서 approvalStatus=APPROVED/PENDING을 각각 조회합니다. " +
                "운영진·코어도 포함하려면 excludeStaff=false를 사용하고 search와 페이지를 확인합니다. 이름·이메일은 name, email을 " +
                "표시하고 기수는 GET /v1/cohorts의 cohortId와 cohortNumber를 매핑합니다. cohortId 자체는 기수 번호가 아닙니다. " +
                "이름·파트 일치만으로 자동 병합하지 말고 동일인 여부와 남길 회원을 확인해야 합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "통합 및 승인 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 요청 또는 병합 대상 조건 불충족 (MEMBER-400-21)"),
        ApiResponse(responseCode = "403", description = "통합·승인 권한 없음"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
        ApiResponse(responseCode = "409", description = "로그인 수단 충돌"),
    )
    fun mergeAndApprove(
        request: MemberMergeRequest,
        @Parameter(hidden = true) actorId: Long,
    ): CustomResponse<Void>

    @Operation(
        summary = "가입 반려",
        description =
            "create:member 권한으로 현재 기수 또는 기수 없는 PENDING 멤버를 반려합니다. 기록을 보존하고 미승인 목록·집계에서 제외합니다. " +
                "기존 이름·파트를 보존하고 프로필 재입력을 허용합니다. 이미 반려되거나 승인된 멤버는 거절합니다. 중복 의심 여부와 관계없이 반려할 수 있으며 반려 사유 입력은 받지 않습니다.",
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
                "반려 후 이름·파트 입력을 완료해야 재신청할 수 있습니다. 단순 재로그인으로 재신청하지 않습니다. " +
                "이미 PENDING인 반복 요청은 추가 신청 기록 없이 성공하며 승인 회원은 거절합니다. " +
                "회원 ID는 토큰에서 가져옵니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "가입 대기 전환 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "재신청할 수 없는 회원 상태"),
        ApiResponse(responseCode = "409", description = "이름·파트 입력 미완료 (MEMBER-409-42)"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "접근 거절"),
        ApiResponse(responseCode = "404", description = "멤버 또는 신청할 기수 없음"),
    )
    fun reapply(
        @Parameter(hidden = true) memberId: Long,
    ): CustomResponse<Void>
}
