package core.application.session.presentation.response

import java.time.LocalDateTime

data class NextSessionResponse(
    val sessionId: Long,
    val week: Int,
    val eventName: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
)
