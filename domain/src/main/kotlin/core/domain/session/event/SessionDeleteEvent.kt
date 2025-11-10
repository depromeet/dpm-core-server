package core.domain.session.event

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionDeleteEvent(
    val sessionId: SessionId,
    val deletedAt: Instant
)

