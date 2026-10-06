package core.application.sessionFeedback.presentation.response

import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import java.time.LocalDateTime

data class SessionListFeedbackResponse(
    val status: SessionFeedbackStatus,
    val endAt: LocalDateTime,
    val canSubmit: Boolean,
)
