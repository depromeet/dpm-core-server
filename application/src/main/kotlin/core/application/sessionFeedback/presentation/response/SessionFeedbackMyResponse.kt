package core.application.sessionFeedback.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import java.time.LocalDateTime

data class SessionFeedbackMyResponse(
    val sessionName: String,
    val myStatus: SessionFeedbackMyStatus,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val startAt: LocalDateTime?,
)
