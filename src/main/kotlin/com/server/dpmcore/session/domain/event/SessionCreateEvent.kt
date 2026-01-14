package com.server.dpmcore.session.domain.event

import com.server.dpmcore.session.domain.model.SessionId

data class SessionCreateEvent(
    val sessionId: SessionId,
)
