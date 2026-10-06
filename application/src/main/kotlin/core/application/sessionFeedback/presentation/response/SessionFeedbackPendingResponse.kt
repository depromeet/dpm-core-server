package core.application.sessionFeedback.presentation.response

import java.time.LocalDateTime

/**
 * 홈 카드용 응답 — 지금 작성 가능한 세션 1건을 담는다.
 *
 * 없으면 `CustomResponse.ok(null)` 로 돌려주고 `data` 키 자체가 생략된다 (`@JsonInclude(NON_NULL)`).
 */
data class SessionFeedbackPendingResponse(
    val sessionId: Long,
    val week: Int,
    val sessionName: String,
    val endAt: LocalDateTime,
)
