package core.application.attendance.presentation.response

data class SessionAttendancesResponse(
    val members: List<MemberAttendanceResponse>,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)
