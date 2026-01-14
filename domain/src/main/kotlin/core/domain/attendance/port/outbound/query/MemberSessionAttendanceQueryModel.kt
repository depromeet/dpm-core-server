package core.domain.attendance.port.outbound.query

import java.time.Instant

data class MemberSessionAttendanceQueryModel(
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: Instant,
    val sessionAttendanceStatus: String,
)
