package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
                description = "인사이트 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "인사이트 예시",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "sessionId": 1,
                                        "sessionName": "디프만 19기 OT",
                                        "status": "IN_PROGRESS",
                                        "startAt": "2026-10-08T00:00:00",
                                        "endAt": "2026-10-11T00:00:00",
                                        "responseSummary": {
                                          "respondentCount": 24,
                                          "targetCount": 30,
                                          "responseRate": 80
                                        },
                                        "satisfaction": {
                                          "average": 4.2,
                                          "maxScore": 5,
                                          "distribution": [
                                            { "code": "VERY_SATISFIED", "label": "매우 만족", "count": 11, "rate": 46 },
                                            { "code": "SATISFIED", "label": "만족", "count": 8, "rate": 33 },
                                            { "code": "NEUTRAL", "label": "보통", "count": 4, "rate": 17 },
                                            { "code": "DISSATISFIED", "label": "불만족", "count": 1, "rate": 4 },
                                            { "code": "VERY_DISSATISFIED", "label": "매우 불만족", "count": 0, "rate": 0 }
                                          ]
                                        },
                                        "likedAspects": {
                                          "respondentCount": 24,
                                          "items": [
                                            { "code": "SESSION_CONTENT", "label": "세션 내용", "count": 15, "rate": 63 },
                                            { "code": "PROGRESS_AND_TIME", "label": "진행 방식·시간", "count": 10, "rate": 42 },
                                            { "code": "NETWORKING", "label": "교류 기회", "count": 5, "rate": 21 },
                                            { "code": "GUIDANCE", "label": "사전·현장 안내", "count": 3, "rate": 13 },
                                            { "code": "PLACE_AND_ACCESS", "label": "장소·접속 환경", "count": 2, "rate": 8 },
                                            { "code": "NOTHING", "label": "특별히 없음", "count": 1, "rate": 4 }
                                          ],
                                          "etcComments": []
                                        },
                                        "improvementAspects": {
                                          "respondentCount": 24,
                                          "items": [
                                            { "code": "PROGRESS_AND_TIME", "label": "진행 방식·시간", "count": 7, "rate": 29 },
                                            { "code": "SESSION_CONTENT", "label": "세션 내용", "count": 5, "rate": 21 },
                                            { "code": "GUIDANCE", "label": "사전·현장 안내", "count": 4, "rate": 17 },
                                            { "code": "NETWORKING", "label": "교류 기회", "count": 3, "rate": 13 },
                                            { "code": "ETC", "label": "기타", "count": 2, "rate": 8 },
                                            { "code": "NOTHING", "label": "특별히 없음", "count": 2, "rate": 8 },
                                            { "code": "PLACE_AND_ACCESS", "label": "장소·접속 환경", "count": 0, "rate": 0 }
                                          ],
                                          "etcComments": [
                                            "질문 시간이 조금 더 길었으면 좋겠어요.",
                                            "세션 자료를 미리 공유해주면 좋겠어요."
                                          ]
                                        },
                                        "freeComments": {
                                          "count": 2,
                                          "items": [
                                            "유익한 세션이었어요.",
                                            "다음에도 이런 형식 좋아요."
                                          ]
                                        }
                                      }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "400", description = "SESSION_FEEDBACK-400-03 피드백 OFF"),
            ApiResponse(responseCode = "403", description = "GLOBAL-403-01 권한 없음"),
            ApiResponse(responseCode = "404", description = "SESSION-404-01 세션 없음"),
        ],
    )
    fun getInsight(sessionId: SessionId): CustomResponse<SessionFeedbackInsightResponse>
}
