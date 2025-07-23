package com.server.dpmcore.session.application

import com.server.dpmcore.session.domain.event.SessionCreateEvent
import com.server.dpmcore.session.domain.exception.InvalidSessionIdException
import com.server.dpmcore.session.domain.exception.SessionNotFoundException
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.domain.port.inbound.command.SessionCreateCommand
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class SessionCommandService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun updateSessionStartTime(
        sessionId: SessionId,
        attendanceStartTime: Instant,
    ) {
        val session =
            sessionPersistencePort.findSessionById(sessionId)
                ?: throw SessionNotFoundException()

        session.updateAttendanceStartTime(attendanceStartTime)

        sessionPersistencePort.save(session)
    }

    fun createSession(command: SessionCreateCommand) {
        val newSession = Session.create(command)

        val savedSession = sessionPersistencePort.save(newSession)

        eventPublisher.publishEvent(SessionCreateEvent(savedSession.id ?: throw InvalidSessionIdException()))
    }
}
