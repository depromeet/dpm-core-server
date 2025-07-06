package com.server.dpmcore.attendance.domain.port.inbound.command

data class AttendanceCreateCommand(
    val sessionId: Long,
    val memberId: Long,
)
