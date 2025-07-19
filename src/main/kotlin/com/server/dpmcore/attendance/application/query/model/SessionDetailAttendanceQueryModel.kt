package com.server.dpmcore.attendance.application.query.model

import java.time.LocalDateTime

data class SessionDetailAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: Int,
    val part: String,
    val lateCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: LocalDateTime,
    val attendanceStatus: String,
    val attendedAt: LocalDateTime,
)
