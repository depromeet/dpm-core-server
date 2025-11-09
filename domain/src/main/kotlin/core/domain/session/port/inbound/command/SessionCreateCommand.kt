package core.domain.session.port.inbound.command

import java.time.Instant

data class SessionCreateCommand(
    val cohortId: Long,
    val date: Instant,
    val week: Int,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
    val attendanceStart: Instant,
    val lateStart: Instant,
    val absentStart: Instant,
)
