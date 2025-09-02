package com.server.dpmcore.session.domain.port.outbound

import com.server.dpmcore.session.domain.model.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    fun save(session: Session): Session
}
