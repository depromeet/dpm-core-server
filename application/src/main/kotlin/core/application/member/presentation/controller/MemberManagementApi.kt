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
                "목록은 이름, 회원 ID 오름차순입니다. 페이지는 1부터 시작합니다. " +
                "중복 의심은 판별 기준 확정 전으로 null을 반환합니다.",
    )
    fun getOverview(request: MemberManagementRequest): CustomResponse<MemberManagementResponse>
}
