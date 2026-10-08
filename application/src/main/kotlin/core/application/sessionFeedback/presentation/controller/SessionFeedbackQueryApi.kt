package core.application.sessionFeedback.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackMyResponse
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
}
