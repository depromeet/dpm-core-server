package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackMyResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackPendingResponse
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Query", description = "세션 피드백 조회 API")
interface SessionFeedbackQueryApi {
    @Operation(
        summary = "피드백 화면 진입",
        description =
            "홈 카드·세션 목록·공유 링크 공통 진입. 세션 이름과 작성 상태(myStatus)·수집 시작 시각(startAt)만 내려준다. " +
                "질문·선택지는 FE 고정. AVAILABLE / BEFORE_START / SUBMITTED / EXPIRED / NOT_TARGET. " +
                "피드백 OFF 세션의 startAt 은 null(키는 포함).",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "진입 성공 (상태 화면도 200)",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "AVAILABLE",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "sessionName": "디프만 19기 OT",
                                        "myStatus": "AVAILABLE",
                                        "startAt": "2026-10-08T00:00:00"
                                      }
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "BEFORE_START",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "sessionName": "디프만 19기 OT",
                                        "myStatus": "BEFORE_START",
                                        "startAt": "2026-10-10T18:00:00"
                                      }
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "NOT_TARGET (피드백 OFF)",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "sessionName": "디프만 19기 OT",
                                        "myStatus": "NOT_TARGET",
                                        "startAt": null
                                      }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getMyFeedback(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<SessionFeedbackMyResponse>

    @Operation(
        summary = "홈 카드용 pending 피드백 조회",
        description =
            "지금 작성 가능한(canSubmit=true) 세션 1건을 반환한다. 여러 건이면 endAt 이 가장 빠른 것. " +
                "없으면 data 키가 생략된 200 응답이 내려간다.",
    )
    fun getPendingFeedback(memberId: MemberId): CustomResponse<SessionFeedbackPendingResponse>
}
