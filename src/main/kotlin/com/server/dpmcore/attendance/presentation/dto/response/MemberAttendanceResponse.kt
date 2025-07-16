package com.server.dpmcore.attendance.presentation.dto.response

data class MemberAttendanceResponse(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)
