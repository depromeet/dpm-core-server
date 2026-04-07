package core.application.session.application.service

import core.application.cohort.application.service.CohortQueryService
import core.application.session.application.exception.InvalidSessionIdException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.application.validator.SessionValidator
import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.notification.vo.SentSessionNotificationId
import core.domain.session.aggregate.Session
import core.domain.session.event.SessionCreateEvent
import core.domain.session.event.SessionDeleteEvent
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
    private val cohortQueryService: CohortQueryService,
    private val sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase,
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
        val latestCohortId = cohortQueryService.getLatestCohortId()
        val newSession = Session.create(command, latestCohortId)

        val savedSession = sessionPersistencePort.save(newSession)

        // 세션 알림 이력 레코드 2개 생성 (ATTENDANCE_STARTED, SESSION_DAY_BEFORE)
        listOf(
            NotificationMessageType.ATTENDANCE_STARTED,
            NotificationMessageType.SESSION_DAY_BEFORE,
        ).forEach { messageType ->
            sentSessionNotificationCommandUseCase.save(
                SentSessionNotification(
                    sentSessionNotificationId = SentSessionNotificationId(0L),
                    sessionId = savedSession.id ?: throw InvalidSessionIdException(),
                    notificationMessageType = messageType,
                    sentAt = null,
                ),
            )
        }

        eventPublisher.publishEvent(
            SessionCreateEvent(
                sessionId = savedSession.id ?: throw InvalidSessionIdException(),
                cohortId = savedSession.cohortId,
            ),
        )
    }

    fun updateSession(command: SessionUpdateCommand) {
        val session =
            sessionPersistencePort.findSessionById(command.sessionId.value)
                ?: throw SessionNotFoundException()

        val previousAttendancePolicy = session.attendancePolicy

        session.updateSession(command)
        sessionPersistencePort.save(session)

        publishIfAttendancePolicyChanged(
            previousAttendancePolicy,
            command.attendanceStart,
            command.lateStart,
            command.absentStart,
            session,
        )
    }

    fun softDeleteSession(sessionId: SessionId) {
        val session =
            sessionPersistencePort.findSessionById(sessionId.value)
                ?: throw SessionNotFoundException()

        val now = Instant.now()

        session.delete(now)
        sessionPersistencePort.save(session)

        eventPublisher.publishEvent(SessionDeleteEvent(sessionId, now))
    }

    private fun publishIfAttendancePolicyChanged(
        previous: AttendancePolicy,
        attendanceStart: Instant,
        lateStart: Instant,
        absentStart: Instant,
        session: Session,
    ) {
        if (
            previous.attendanceStart == attendanceStart &&
            previous.lateStart == lateStart &&
            previous.absentStart == absentStart
        ) {
            return
        }

        val sessionId = session.id ?: throw InvalidSessionIdException()

        val event =
            SessionUpdateEvent(
                sessionId = sessionId,
                lateStart = lateStart,
                absentStart = absentStart,
            )

        eventPublisher.publishEvent(event)
    }
}
