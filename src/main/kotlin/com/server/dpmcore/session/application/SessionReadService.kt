package com.server.dpmcore.session.application

import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class SessionReadService(
    private val sessionPersistencePort: SessionPersistencePort,
) {
    fun getNextSession(): Session? {
        val currentTime = Instant.now()

        return sessionPersistencePort.findNextSessionBy(currentTime)
    }
}
