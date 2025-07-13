package com.server.dpmcore.attendance.presentation.dto.response

data class SessionAttendanceResponse(
    val members: List<MemberAttendanceResponse>,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)
