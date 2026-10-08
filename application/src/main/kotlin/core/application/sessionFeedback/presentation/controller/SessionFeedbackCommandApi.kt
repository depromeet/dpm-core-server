package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.request.SessionFeedbackCreateRequest
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Command", description = "세션 피드백 제출 API")
interface SessionFeedbackCommandApi {
    @Operation(
        summary = "피드백 제출",
        description =
            "세션당 멤버 1건만 허용한다. 성공 시 HTTP 200, GLOBAL-200-01, data 없음. " +
                "검증은 세션 → OFF → 대상 → 중복 → 기간 → 응답값 순서. 중복 제출은 SESSION_FEEDBACK-409-01.",
    )
    fun submitFeedback(
        sessionId: SessionId,
        memberId: MemberId,
        request: SessionFeedbackCreateRequest,
    ): CustomResponse<Void>
}
