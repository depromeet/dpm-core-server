package core.application.sessionFeedback.presentation.response

import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus

data class SessionFeedbackMyResponse(
    val sessionName: String,
    val myStatus: SessionFeedbackMyStatus,
)
