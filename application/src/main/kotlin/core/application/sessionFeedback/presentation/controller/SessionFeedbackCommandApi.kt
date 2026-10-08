package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.request.SessionFeedbackCreateRequest
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Command", description = "세션 피드백 제출 API")
interface SessionFeedbackCommandApi {
    @Operation(
        summary = "피드백 제출",
        description =
            "세션당 멤버 1건만 허용한다. 성공 시 HTTP 200, GLOBAL-200-01, data 없음. " +
                "검증 순서: 세션 → OFF → 대상 → 중복 → 기간 → 응답값. " +
                "likedAspects/improvementAspects 는 1~2개, ETC 선택 시 기타 의견 필수, NOTHING 은 단독만.",
        requestBody =
            RequestBody(
                description = "피드백 제출 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = SessionFeedbackCreateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "제출 예시 (기타 포함)",
                                value = """
                                {
                                  "satisfaction": "VERY_SATISFIED",
                                  "likedAspects": ["SESSION_CONTENT", "ETC"],
                                  "likedEtc": "현직자 질의응답이 좋았어요.",
                                  "improvementAspects": ["PROGRESS_AND_TIME"],
                                  "improvementEtc": null,
                                  "freeComment": "질문 시간이 조금 더 길었으면 좋겠어요."
                                }
                            """,
                            ),
                            ExampleObject(
                                name = "제출 예시 (특별히 없음)",
                                value = """
                                {
                                  "satisfaction": "NEUTRAL",
                                  "likedAspects": ["NOTHING"],
                                  "likedEtc": null,
                                  "improvementAspects": ["ETC", "GUIDANCE"],
                                  "improvementEtc": "세션 자료를 미리 공유해주면 좋겠어요.",
                                  "freeComment": null
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "제출 성공 (data 없음)",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "성공",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description =
                    "SESSION_FEEDBACK-400-03~09, GLOBAL-400-01 (형식 오류·리스트 null 요소)",
            ),
            ApiResponse(responseCode = "403", description = "SESSION_FEEDBACK-403-01 대상 아님"),
            ApiResponse(responseCode = "409", description = "SESSION_FEEDBACK-409-01 이미 제출"),
        ],
    )
    fun submitFeedback(
        sessionId: SessionId,
        memberId: MemberId,
        request: SessionFeedbackCreateRequest,
    ): CustomResponse<Void>
}
