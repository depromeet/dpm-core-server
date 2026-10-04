package core.domain.session.port.inbound.command

import java.time.Instant

/** 출석 시각은 모두 생략(서버 기본값)하거나 모두 제공해야 한다. */
data class SessionCreateCommand(
    val date: Instant,
    val week: Int,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
    val attendanceStart: Instant? = null,
    val lateStart: Instant? = null,
    val absentStart: Instant? = null,
)
