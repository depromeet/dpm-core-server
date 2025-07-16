package com.server.dpmcore.attendance.domain.port.inbound.query

import com.server.dpmcore.attendance.domain.model.AttendanceStatus

data class GetMemberAttendancesQuery(
    val statuses: List<AttendanceStatus>?,
    val teams: List<Int>?,
    val name: String?,
    val cursorId: Long?,
)
