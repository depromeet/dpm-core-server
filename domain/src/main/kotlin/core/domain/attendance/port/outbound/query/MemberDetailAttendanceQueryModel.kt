package core.domain.attendance.port.outbound.query

import core.domain.team.vo.TeamNumber

data class MemberDetailAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val summary: AttendanceSummaryQueryModel,
)
