package com.server.dpmcore.attendance.presentation.dto.response

data class SessionAttendancesResponse(
    val members: List<MemberAttendanceResponse>,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)
