package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberManagementRequest
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
                "중복 의심은 탭·검색·필터 적용 전 관리 대상 중 닉네임과 파트가 같은 다른 회원이 있으면 true입니다.",
    )
    fun getOverview(request: MemberManagementRequest): CustomResponse<MemberManagementResponse>
}
