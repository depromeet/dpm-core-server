package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Insight", description = "세션 피드백 인사이트(운영진) API")
interface SessionFeedbackInsightApi {
    @Operation(
        summary = "운영진 피드백 인사이트 조회",
        description =
            "제출 즉시 실시간 누적 집계 결과를 반환한다. 수집 중에도 조회 가능. " +
                "응답자 식별 정보는 포함하지 않는다. 피드백 OFF 세션은 SESSION_FEEDBACK-400-03 로 응답한다.",
    )
    fun getInsight(sessionId: SessionId): CustomResponse<SessionFeedbackInsightResponse>
}
