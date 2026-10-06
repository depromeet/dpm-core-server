package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackMyResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackPendingResponse
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Feedback Query", description = "세션 피드백 조회 API")
interface SessionFeedbackQueryApi {
    @Operation(
        summary = "피드백 화면 진입",
        description =
            "홈 카드·세션 목록·공유 링크 공통 진입. 상태 화면은 에러가 아니라 200 + myStatus 로 응답한다. " +
                "AVAILABLE 일 때만 questions 가 함께 내려간다.",
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
