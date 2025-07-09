package com.server.dpmcore.attendance.presentation.dto.request

data class AttendanceCreateRequest(
    val memberId: Long,
    val attendanceCode: String,
)
