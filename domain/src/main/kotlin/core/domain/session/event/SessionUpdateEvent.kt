package core.domain.session.event

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionUpdateEvent(
    val sessionId: SessionId,
    val attendanceStart: UpdateTime,
    val lateStart: UpdateTime,
    val absentStart: UpdateTime,
) {
    data class UpdateTime(
        val from: Instant,
        val to: Instant,
    )
}
