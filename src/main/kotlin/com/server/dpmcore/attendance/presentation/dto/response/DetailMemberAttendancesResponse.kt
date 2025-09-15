package com.server.dpmcore.attendance.presentation.dto.response

import java.time.LocalDateTime

data class DetailMemberAttendancesResponse(
    val member: DetailMemberInfo,
    val attendance: MemberDetailAttendanceCountInfo,
    val sessions: List<MemberDetailSessionInfo>,
)

data class DetailMemberInfo(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)

data class MemberDetailAttendanceCountInfo(
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val absentCount: Int,
    val earlyLeaveCount: Int,
)

data class MemberDetailSessionInfo(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: LocalDateTime,
    val attendanceStatus: String,
)
