package com.server.dpmcore.attendance.presentation.dto.response

import java.time.LocalDateTime

data class MyDetailAttendanceBySessionResponse(
    val attendance: MyDetailAttendanceInfo,
    val session: MyDetailAttendanceSessionInfo,
)

data class MyDetailAttendanceInfo(
    val status: String,
    val attendedAt: LocalDateTime?,
)

data class MyDetailAttendanceSessionInfo(
    val week: Int,
    val eventName: String,
    val date: LocalDateTime,
    val place: String,
)
