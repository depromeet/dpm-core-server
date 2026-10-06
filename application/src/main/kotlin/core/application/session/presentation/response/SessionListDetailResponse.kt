package core.application.session.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.application.sessionFeedback.presentation.response.SessionListFeedbackResponse
import java.time.LocalDateTime

data class SessionListDetailResponse(
    val id: Long,
    val week: Int,
    val name: String,
    val date: LocalDateTime,
    val place: String?,
    val isOnline: Boolean,
    /** 피드백 받기 OFF 세션은 null. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val feedback: SessionListFeedbackResponse? = null,
)
