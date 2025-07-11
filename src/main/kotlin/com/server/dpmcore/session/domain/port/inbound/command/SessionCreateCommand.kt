package com.server.dpmcore.session.domain.port.inbound.command

import java.time.Instant

data class SessionCreateCommand(
    val cohortId: Long,
    val date: Instant,
    val week: Int,
    val startHour: Long,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
)
