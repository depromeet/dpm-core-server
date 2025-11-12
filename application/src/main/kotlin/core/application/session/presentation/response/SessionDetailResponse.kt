package core.application.session.presentation.response

import java.time.LocalDateTime

data class SessionDetailResponse(
    val sessionId: Long,
    val week: Int,
    val eventName: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
    val attendanceCode: String,
)
