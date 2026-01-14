package core.application.session.presentation.response

import java.time.LocalDateTime

data class NextSessionResponse(
    val id: Long,
    val week: Int,
    val name: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
    val attendanceCode: String,
)
