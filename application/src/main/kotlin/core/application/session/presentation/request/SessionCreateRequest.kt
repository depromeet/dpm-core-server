package core.application.session.presentation.request

import java.time.LocalDateTime

data class SessionCreateRequest(
    val name: String,
    val date: LocalDateTime,
    val isOnline: Boolean? = false,
    val place: String?,
    val week: Int,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
)
