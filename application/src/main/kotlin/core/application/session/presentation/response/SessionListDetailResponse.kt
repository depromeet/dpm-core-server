package core.application.session.presentation.response

import java.time.LocalDateTime

data class SessionListDetailResponse(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: LocalDateTime,
)
