package core.application.attendance.presentation.request

import java.time.LocalDateTime

data class UpdateAttendanceTimeRequest(
    val attendanceStartTime: LocalDateTime,
)
