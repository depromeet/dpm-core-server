package core.domain.session.port.outbound

import core.domain.session.aggregate.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    fun save(session: Session): Session
}
