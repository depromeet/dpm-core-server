package core.domain.session.event

import core.domain.session.vo.SessionId

data class SessionCreateEvent(
    val sessionId: SessionId,
)
