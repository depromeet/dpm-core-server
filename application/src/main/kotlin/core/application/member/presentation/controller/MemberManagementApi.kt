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
            "현재 기수와 기수 없는 가입 대기자를 조회합니다. 요약은 검색·필터와 무관하며 " +
                "목록은 닉네임, 회원 ID 오름차순입니다. 페이지는 1부터 시작합니다. " +
                "parts·teamNumbers·activityStatuses는 반복 파라미터 또는 쉼표로 여러 값을 전달합니다. " +
                "같은 필터 안에서는 OR, 필터 사이는 AND이며 생략하거나 빈 리스트면 적용하지 않습니다. " +
                "중복 의심은 탭·검색·필터 적용 전 관리 대상 중 닉네임과 파트가 같은 다른 회원이 있으면 true입니다.",
    )
    fun getOverview(request: MemberManagementRequest): CustomResponse<MemberManagementResponse>

    @Operation(
        summary = "승인된 멤버 정보 수정",
        description =
            "현재 기수의 ACTIVE/INACTIVE 멤버만 수정합니다. 지정한 컬럼을 한 트랜잭션에서 변경하며 생략/null은 유지합니다. " +
                "타입 변경은 update:member와 update:authorization 권한이 모두 필요합니다. 닉네임과 수료 상태는 수정하지 않습니다.",
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
