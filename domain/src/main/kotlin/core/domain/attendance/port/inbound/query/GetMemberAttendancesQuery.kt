package core.domain.attendance.port.inbound.query

import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId

data class GetMemberAttendancesQuery(
    val memberId: MemberId,
    val statuses: List<AttendanceStatus>?,
    val teams: List<Int>?,
    val name: String?,
    val onlyMyTeam: Boolean?,
    val page: Int,
    val size: Int,
)
