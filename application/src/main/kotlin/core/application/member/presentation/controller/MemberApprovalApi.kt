package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberApprovalRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberApprovalApi {
    @Operation(
        summary = "가입 대기 멤버 및 지난 기수 멤버의 현재 기수 단건·일괄 승인",
        description =
            "이름·파트 제출을 완료한 현재 기수 또는 기수 없는 PENDING 멤버를 승인합니다. 닉네임·파트는 유지합니다. " +
                "memberType·teamId를 생략하면 현재 기수의 디퍼, 팀 미배정으로 설정합니다. 지정한 타입·팀은 요청한 모든 멤버에게 같이 적용합니다. " +
                "ORGANIZER·CORE 지정은 update:authorization 권한이 필요하며 없으면 403입니다. " +
                "현재 기수 팀이 아닌 teamId는 대상 전원이 이미 현재 기수 멤버여도 400 MEMBER-400-08로 전체 거절합니다. " +
                "미제출 PENDING 회원은 409 MEMBER-409-42로 거절합니다. 중복 의심은 승인 차단 조건이 아닙니다. " +
                "현재 기수 ACTIVE 재요청은 타입·팀을 보내도 변경하지 않으며, 수정은 PATCH /v3/members/{memberId}를 사용합니다. " +
                "같은 기수의 SUSPENDED/ENDED " +
                "복구는 409 MEMBER-409-61로 차단합니다. 이전 기수 승인 회원은 현재 활성 기수에 새로 승인할 수 있으며 과거 활동 이력을 " +
                "유지합니다. 지난 기수에 속한 소속·팀·역할·출석 기록은 변경하지 않습니다. 잘못된 대상이나 초기화 실패가 있으면 전체 취소합니다. " +
                "과제·회식은 현재 MVP 미사용 기능이며, 2차 MVP 검토 전까지 기존 승인 초기화는 유지합니다. " +
                "중복 의심 여부는 승인을 막지 않으며 계정을 자동 병합하지 않습니다.",
    )
    fun approve(request: MemberApprovalRequest): CustomResponse<Void>
}
