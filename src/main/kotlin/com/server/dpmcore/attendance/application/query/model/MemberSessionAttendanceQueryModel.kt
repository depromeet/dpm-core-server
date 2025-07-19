package com.server.dpmcore.attendance.application.query.model

import java.time.LocalDateTime

data class MemberSessionAttendanceQueryModel(
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: LocalDateTime,
    val sessionAttendanceStatus: String,
)
