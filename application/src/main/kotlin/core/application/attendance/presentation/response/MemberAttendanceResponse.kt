package core.application.attendance.presentation.response

import core.domain.team.vo.TeamNumber

data class MemberAttendanceResponse(
    val id: Long,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val attendanceStatus: String,
)
