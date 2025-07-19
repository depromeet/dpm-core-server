package com.server.dpmcore.attendance.presentation.dto.response

data class DetailMemberAttendancesResponse(
    val member: DetailMember,
    val attendance: DetailAttendance,
    val sessions: List<DetailSession>,
)

data class DetailMember(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)

data class DetailAttendance(
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val absentCount: Int,
)

data class DetailSession(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: String,
    val attendanceStatus: String,
)
