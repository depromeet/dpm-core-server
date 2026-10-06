package core.application.sessionFeedback.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import java.time.LocalDateTime

/**
 * 피드백 화면 진입 응답. 홈 카드·세션 목록·공유 링크가 공통으로 사용한다.
 *
 * - `myStatus` 가 `AVAILABLE` 일 때만 `questions` 가 포함되고, 그 외엔 null.
 * - `startAt`/`endAt` 는 세션의 피드백 받기 OFF (`DISABLED`) 세션에서 null 로 내려간다.
 */
data class SessionFeedbackMyResponse(
    val sessionId: Long,
    val week: Int,
    val sessionName: String,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val startAt: LocalDateTime?,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val endAt: LocalDateTime?,
    val myStatus: SessionFeedbackMyStatus,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val questions: SessionFeedbackQuestionsResponse?,
)
