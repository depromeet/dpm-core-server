package com.server.dpmcore.attendance.domain.port.inbound.query

import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.member.member.domain.model.MemberId

data class GetMemberAttendancesQuery(
    val memberId: MemberId,
    val statuses: List<AttendanceStatus>?,
    val teams: List<Int>?,
    val name: String?,
    val onlyMyTeam: Boolean?,
    val cursorId: Long?,
)
