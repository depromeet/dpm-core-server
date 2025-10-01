package core.domain.attendance.port.inbound.query

import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class GetAttendancesBySessionWeekQuery(
    val sessionId: SessionId,
    val memberId: MemberId,
    val statuses: List<AttendanceStatus>?,
    val teams: List<Int>?,
    val name: String?,
    val onlyMyTeam: Boolean?,
    val cursorId: Long?,
)
