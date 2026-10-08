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
            "홈 카드·세션 목록·공유 링크 공통 진입. 세션 이름과 작성 상태(myStatus)만 내려준다. " +
                "AVAILABLE(작성 가능), BEFORE_START(수집 시작 전), SUBMITTED(이미 제출함), " +
                "EXPIRED(제출 기한 지남), NOT_TARGET(제출 자격 없음). startAt 은 수집 시작 시각(피드백 OFF 세션은 null).",
    )
    fun getMyFeedback(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<SessionFeedbackMyResponse>
}
