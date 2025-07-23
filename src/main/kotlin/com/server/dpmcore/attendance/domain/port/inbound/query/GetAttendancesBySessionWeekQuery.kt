package com.server.dpmcore.attendance.domain.port.inbound.query

import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.session.domain.model.SessionId

data class GetAttendancesBySessionWeekQuery(
    val sessionId: SessionId,
    val statuses: List<AttendanceStatus>?,
    val teams: List<Int>?,
    val name: String?,
    val cursorId: Long?,
)
