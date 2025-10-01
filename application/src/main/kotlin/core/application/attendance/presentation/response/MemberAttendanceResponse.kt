package core.application.attendance.presentation.response

data class MemberAttendanceResponse(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)
