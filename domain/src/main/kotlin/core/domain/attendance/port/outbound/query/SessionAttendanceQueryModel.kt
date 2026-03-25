package core.domain.attendance.port.outbound.query

import core.domain.team.vo.TeamNumber

data class SessionAttendanceQueryModel(
    val id: Long,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val attendanceStatus: String,
)
