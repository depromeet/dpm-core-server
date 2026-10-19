package core.domain.attendance.port.outbound.query

import core.domain.team.vo.TeamNumber

data class MemberAttendanceQueryModel(
    val id: Long,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val summary: AttendanceSummaryQueryModel,
)
