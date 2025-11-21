package core.application.session.presentation.response

import java.time.LocalDateTime

data class SessionListDetailResponse(
    val id: Long,
    val week: Int,
    val name: String,
    val date: LocalDateTime,
    val place: String?,
    val isOnline: Boolean,
)
