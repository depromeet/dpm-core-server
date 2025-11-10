package core.domain.session.event

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionUpdateEvent(
    val sessionId: SessionId,
    val lateStart: Instant,
    val absentStart: Instant,
)
