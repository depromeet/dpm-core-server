package core.domain.session.port.inbound.command

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionAttendancePolicyCommand(
    val sessionId: SessionId,
    val attendanceStart: Instant,
    val lateStart: Instant,
    val absentStart: Instant,
)
