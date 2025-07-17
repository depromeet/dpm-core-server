package com.server.dpmcore.attendance.presentation.dto.request

import java.time.LocalDateTime

data class UpdateAttendanceTimeRequest(
    val attendanceStartTime: LocalDateTime,
)
