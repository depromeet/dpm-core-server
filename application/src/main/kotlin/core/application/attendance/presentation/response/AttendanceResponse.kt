package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class AttendanceResponse(
    val attendanceStatus: String,
    val attendedAt: LocalDateTime,
)
