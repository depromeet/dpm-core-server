package core.application.session.presentation.response

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
)
