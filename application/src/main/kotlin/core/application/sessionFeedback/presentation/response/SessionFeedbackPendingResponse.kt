package core.application.sessionFeedback.presentation.response

import java.time.LocalDateTime

data class SessionFeedbackPendingResponse(
    val sessionId: Long,
    val week: Int,
    val sessionName: String,
    val endAt: LocalDateTime,
)
