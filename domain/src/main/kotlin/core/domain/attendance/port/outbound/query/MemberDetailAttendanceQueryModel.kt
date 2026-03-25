package core.domain.attendance.port.outbound.query

import core.domain.team.vo.TeamNumber

data class MemberDetailAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
    val earlyLeaveCount: Int,
)
