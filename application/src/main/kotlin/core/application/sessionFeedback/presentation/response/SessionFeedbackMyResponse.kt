package core.application.sessionFeedback.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import java.time.LocalDateTime

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
