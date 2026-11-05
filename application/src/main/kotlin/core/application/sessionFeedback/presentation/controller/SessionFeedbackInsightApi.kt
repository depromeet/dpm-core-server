package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Insight", description = "세션 피드백 인사이트(운영진) API")
interface SessionFeedbackInsightApi {
    @Operation(
        summary = "운영진 피드백 인사이트 조회",
        description =
            "제출 즉시 실시간 누적 집계 결과를 반환한다. 수집 중에도 조회 가능. " +
                "응답자 식별 정보는 포함하지 않는다. 피드백 OFF 세션은 SESSION_FEEDBACK-400-03. " +
                "비율은 반올림. liked/improvement 0명 항목 포함(ETC 는 0명이면 제외).",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인사이트 조회 성공 (CustomResponse<SessionFeedbackInsightResponse>)",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "SESSION_FEEDBACK-400-03 피드백 OFF",
                content = [Content(schema = Schema(implementation = CustomResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "GLOBAL-403-01 권한 없음",
                content = [Content(schema = Schema(implementation = CustomResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "SESSION-404-01 세션 없음",
                content = [Content(schema = Schema(implementation = CustomResponse::class))],
            ),
        ],
    )
    fun getInsight(sessionId: SessionId): CustomResponse<SessionFeedbackInsightResponse>
}
