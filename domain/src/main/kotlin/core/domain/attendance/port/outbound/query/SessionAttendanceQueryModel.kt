package core.domain.attendance.port.outbound.query

data class SessionAttendanceQueryModel(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val attendanceStatus: String,
)
