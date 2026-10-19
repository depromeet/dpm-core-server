package core.application.session.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import core.application.sessionFeedback.presentation.response.SessionFeedbackSettingsResponse
import java.time.LocalDateTime

data class SessionDetailResponse(
    val id: Long,
    val week: Int,
    val name: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
    val attendanceCode: String,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val feedback: SessionFeedbackSettingsResponse? = null,
)
