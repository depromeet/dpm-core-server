package com.server.dpmcore.attendance.application.query.model

data class SessionAttendanceQueryModel(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)
