package core.application.sessionFeedback.presentation.response

import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import java.time.LocalDateTime

/**
 * 세션 목록 응답에 포함되는 피드백 상태. 피드백 받기 OFF 세션에선 상위 필드가 `null` 로 생략된다.
 *
 * - `canSubmit` 는 로그인한 멤버 기준: 수집 중 + 출석/지각 대상 + 미제출일 때만 `true`. 비로그인은 항상 `false`.
 */
data class SessionListFeedbackResponse(
    val status: SessionFeedbackStatus,
    val endAt: LocalDateTime,
    val canSubmit: Boolean,
)
