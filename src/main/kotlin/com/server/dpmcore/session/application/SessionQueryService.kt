package com.server.dpmcore.session.application

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.session.domain.exception.SessionNotFoundException
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class SessionQueryService(
    private val sessionPersistencePort: SessionPersistencePort,
) {
    fun getNextSession(): Session? {
        val currentTime = Instant.now()

        return sessionPersistencePort.findNextSessionBy(currentTime)
    }

    fun getAllSessions(cohortId: CohortId): List<Session> = sessionPersistencePort.findAllSessions(cohortId)

    fun getSessionById(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionById(sessionId)
            ?: throw SessionNotFoundException()
}
