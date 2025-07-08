package com.server.dpmcore.session.domain.port.outbound

import com.server.dpmcore.session.domain.model.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(currentTime: Instant): Session?
}
