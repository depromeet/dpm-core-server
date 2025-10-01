package core.application.attendance.presentation.response

data class MemberAttendancesResponse(
    val members: List<MemberAttendanceResponse>,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)
