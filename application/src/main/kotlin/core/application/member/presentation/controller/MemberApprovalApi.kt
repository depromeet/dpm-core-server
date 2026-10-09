package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberApprovalRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberApprovalApi {
    @Operation(
        summary = "가입 대기 멤버 단건 및 일괄 승인",
        description =
            "현재 기수 또는 기수 없는 PENDING 멤버를 승인합니다. 닉네임·파트는 유지하고 현재 기수의 디퍼, 팀 미배정으로 설정합니다. " +
                "현재 기수의 ACTIVE/INACTIVE 멤버 재요청은 변경하지 않습니다. 잘못된 대상이나 초기화 실패가 있으면 전체 취소합니다. " +
                "과제·회식은 현재 MVP 미사용 기능이며, 2차 MVP 검토 전까지 기존 승인 초기화는 유지합니다. " +
                "중복 의심 여부는 승인을 막지 않으며 계정을 자동 병합하지 않습니다.",
    )
    fun approve(request: MemberApprovalRequest): CustomResponse<Void>
}
