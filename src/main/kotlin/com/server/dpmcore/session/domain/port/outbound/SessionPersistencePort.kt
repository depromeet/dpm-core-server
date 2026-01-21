package com.server.dpmcore.session.domain.port.outbound

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllSessions(cohortId: CohortId): List<Session>

    fun findSessionById(sessionId: SessionId): Session?

    fun save(session: Session): Session

    fun findAllSessionWeeks(cohortId: CohortId): List<Int>
}
