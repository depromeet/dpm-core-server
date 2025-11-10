package core.application.session.application.service

import core.application.session.application.exception.InvalidSessionIdException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.application.validator.SessionValidator
import core.domain.session.aggregate.Session
import core.domain.session.event.SessionCreateEvent
import core.domain.session.event.SessionUpdateEvent
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class SessionCommandService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val eventPublisher: ApplicationEventPublisher,
    private val sessionValidator: SessionValidator,
) {
    fun updateSessionStartTime(
        sessionId: SessionId,
        attendanceStartTime: Instant,
    ) {
        val session =
            sessionPersistencePort.findSessionById(sessionId.value)
                ?: throw SessionNotFoundException()

        sessionValidator.validateIsSameDateAsSession(session, attendanceStartTime)
        session.updateAttendanceStartTime(attendanceStartTime)

        sessionPersistencePort.save(session)
    }

    fun createSession(command: SessionCreateCommand) {
        val newSession = Session.create(command)

        val savedSession = sessionPersistencePort.save(newSession)

        eventPublisher.publishEvent(SessionCreateEvent(savedSession.id ?: throw InvalidSessionIdException()))
    }

    fun updateSession(command: SessionUpdateCommand) {
        val session =
            sessionPersistencePort.findSessionById(command.sessionId.value)
                ?: throw SessionNotFoundException()

        val previousAttendancePolicy = session.attendancePolicy

        session.updateSession(command)
        sessionPersistencePort.save(session)

        publishIfAttendancePolicyChanged(previousAttendancePolicy, command, session)
    }

    private fun publishIfAttendancePolicyChanged(
        previous: AttendancePolicy,
        command: SessionUpdateCommand,
        session: Session,
    ) {
        val changed = hasPolicyChanged(previous, command)
        if (!changed) return

        val sessionId = session.id ?: throw InvalidSessionIdException()

        val event = SessionUpdateEvent(
            sessionId = sessionId,
            attendanceStart = SessionUpdateEvent.UpdateTime(
                from = previous.attendanceStart,
                to = command.attendanceStart,
            ),
            lateStart = SessionUpdateEvent.UpdateTime(
                from = previous.lateStart,
                to = command.lateStart,
            ),
            absentStart = SessionUpdateEvent.UpdateTime(
                from = previous.absentStart,
                to = command.absentStart,
            ),
        )

        eventPublisher.publishEvent(event)
    }

    private fun hasPolicyChanged(previous: AttendancePolicy, command: SessionUpdateCommand): Boolean {
        return previous.attendanceStart != command.attendanceStart ||
                previous.lateStart != command.lateStart ||
                previous.absentStart != command.absentStart
    }

}
