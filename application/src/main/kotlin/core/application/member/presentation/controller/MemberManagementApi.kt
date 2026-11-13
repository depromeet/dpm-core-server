package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberManagementBulkUpdateRequest
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.application.member.presentation.response.MemberManagementResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberManagementApi {
    @Operation(
        summary = "멤버 관리 목록 및 요약 조회",
        description =
            "cohortId 기수(생략 시 활성 기수)의 승인 회원과 가입 대기자를 조회합니다. cohortId가 1 미만이면 400, " +
                "없는 기수는 404(COHORT-404-1)입니다. 팀 번호·멤버 타입·활동 상태와 요약은 조회한 기수 기준이며 " +
                "응답 cohortId도 조회한 기수입니다. 과거 기수의 수료 상태는 판정하지 않고 COMPLETED로 표시하므로 " +
                "수료 위험 필터와 graduationRiskCount는 0건입니다. status와 updatedAt은 기수별 값이 아닌 회원 전체 값이며, " +
                "탈퇴·반려·삭제된 회원은 과거 기수에서도 제외합니다. 가입 대기자는 승인 가능한 활성 기수 소속이거나 " +
                "기수 소속이 없는 회원이며 cohortId와 관계없이 같습니다. 멤버 수정은 활성 기수만 가능하므로 " +
                "과거 기수 목록은 조회 전용입니다. PENDING 회원은 이름·파트 제출을 완료한 경우만 포함합니다. " +
                "signupEmail 이 @dpm.test 인 QA 계정은 목록·요약에서 제외합니다(로그인·마이페이지는 가능). " +
                "요약은 검색·필터와 무관하며 " +
                "미승인(PENDING) 목록은 최근 가입·재신청 요청순입니다. 재신청 이력이 있으면 최신 재신청 시각, " +
                "없으면 최초 프로필 제출 시각을 사용하며 동률은 회원 ID 내림차순입니다. 시각 없는 기존 회원은 마지막에 표시합니다. " +
                "이름·파트 수정은 순서에 영향을 주지 않습니다. 승인(APPROVED) 목록은 닉네임, 회원 ID 오름차순입니다. " +
                "필터·정렬 후 페이지를 나누며 페이지는 1부터 시작합니다. " +
                "parts·teamNumbers·activityStatuses·memberTypes는 반복 파라미터 또는 쉼표로 여러 값을 전달합니다. " +
                "같은 필터 안에서는 OR, 필터 사이는 AND입니다. parts·teamNumbers·activityStatuses는 " +
                "생략하거나 빈 리스트면 적용하지 않습니다. memberTypes를 지정하면 excludeStaff보다 우선하며, " +
                "생략하거나 빈 리스트면 excludeStaff 기준으로 조회해 기본값에서는 디퍼와 타입 미배정 회원만 조회합니다. " +
                "활동 상태는 activityStatus로 구분하며 SUSPENDED/ENDED는 수료 위험 인원에서 제외합니다. INACTIVE 필터는 " +
                "ENDED의 호환 별칭입니다. 중복 의심은 탭·검색·필터 적용 전 관리 대상 중 닉네임과 파트가 같은 다른 회원이 있으면 " +
                "true입니다.",
    )
    fun getOverview(request: MemberManagementRequest): CustomResponse<MemberManagementResponse>

    @Operation(
        summary = "승인된 멤버 정보 수정",
        description =
            "현재 기수의 ACTIVE/INACTIVE 멤버만 수정합니다. 지정한 컬럼을 한 트랜잭션에서 변경하며 생략/null은 유지합니다. " +
                "activityStatus=SUSPENDED는 활동 정지, ENDED는 기수 종료입니다. 같은 기수에서는 ACTIVE로 복구할 수 " +
                "없으며 409 MEMBER-409-61을 반환합니다. 정지/종료 후에도 로그인·본인 정보 조회는 가능하지만 활동·관리 권한은 " +
                "차단합니다. 타입 변경은 update:member와 update:authorization 권한이 모두 필요합니다. 닉네임과 수료 상태는 " +
                "수정하지 않습니다.",
    )
    fun updateMember(
        memberId: Long,
        request: MemberManagementUpdateRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "승인된 멤버 정보 컬럼별 일괄 수정",
        description =
            "changes에 한 컬럼만 지정합니다. 하나라도 수정할 수 없는 대상이거나 처리에 실패하면 전체 취소합니다. " +
                "팀·타입은 현재 기수만 변경하고 과거 기수 이력을 보존합니다. 기수 없는 레거시 타입은 현재 기수 타입으로 교체합니다.",
    )
    fun updateMembers(request: MemberManagementBulkUpdateRequest): CustomResponse<Void>
}
