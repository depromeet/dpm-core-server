package core.application.session.application.service

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.attendance.application.service.AttendanceCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.session.application.exception.InvalidSessionIdException
import core.application.session.application.exception.PartialAttendanceTimesException
import core.application.session.application.exception.SessionNotFoundException
import core.application.session.application.validator.SessionValidator
import core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService
import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.notification.vo.SentSessionNotificationId
import core.domain.session.aggregate.Session
import core.domain.session.event.SessionCreateEvent
import core.domain.session.event.SessionDeleteEvent
import core.domain.session.event.SessionUpdateEvent
import core.domain.session.extension.hasChangedComparedTo
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.session.vo.SessionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 세션 행 쓰기 잠금을 먼저 잡고 출석 행을 바꿔 인증/자동 결석/운영진 변경과 직렬화한다
 * (잠금 순서: session -> attendance).
 */
@Service
@Transactional
class SessionCommandService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val eventPublisher: ApplicationEventPublisher,
    private val sessionValidator: SessionValidator,
    private val cohortQueryService: CohortQueryService,
    private val sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase,
    private val attendancePolicyProperties: AttendancePolicyProperties,
    private val attendanceCommandService: AttendanceCommandService,
    private val sessionFeedbackFormCommandService: SessionFeedbackFormCommandService,
    private val clock: Clock,
) {
    // 판정 경계(지각 시작, 마감)는 그대로라 출석 기록은 재판정하지 않는다.
    fun updateSessionStartTime(
        sessionId: SessionId,
        attendanceStartTime: Instant,
    ) {
        val session =
            sessionPersistencePort.findSessionByIdForUpdate(sessionId.value)
                ?: throw SessionNotFoundException()

        sessionValidator.validateAttendanceTimes(
            attendanceStartTime,
            session.attendancePolicy.lateStart,
            session.attendancePolicy.absentStart,
        )
        session.updateAttendanceStartTime(attendanceStartTime)

        sessionPersistencePort.save(session)
    }

    fun createSession(command: SessionCreateCommand) {
        val latestCohortId = cohortQueryService.getLatestCohortId()
        val attendanceTimes = resolveAttendanceTimes(command)
        val newSession = Session.create(command, latestCohortId, attendanceTimes)

        val savedSession = sessionPersistencePort.save(newSession)
        val savedSessionId = savedSession.id ?: throw InvalidSessionIdException()

        sessionFeedbackFormCommandService.applyOnSessionCreate(
            sessionId = savedSessionId,
            feedbackEnabled = command.feedbackEnabled,
            feedbackStartAt = command.feedbackStartAt,
            feedbackPushEnabled = command.feedbackPushEnabled,
        )

        // 세션 알림 이력 레코드 2개 생성 (ATTENDANCE_STARTED, SESSION_DAY_BEFORE)
        listOf(
            NotificationMessageType.ATTENDANCE_STARTED,
            NotificationMessageType.SESSION_DAY_BEFORE,
        ).forEach { messageType ->
            sentSessionNotificationCommandUseCase.save(
                SentSessionNotification(
                    sentSessionNotificationId = SentSessionNotificationId(0L),
                    sessionId = savedSessionId,
                    notificationMessageType = messageType,
                    sentAt = null,
                ),
            )
        }

        eventPublisher.publishEvent(
            SessionCreateEvent(
                sessionId = savedSessionId,
                cohortId = savedSession.cohortId,
            ),
        )
    }

    // 오래된 이벤트 값이 이후 변경을 덮어쓰지 않도록 같은 트랜잭션에서 잠금을 유지한 채 재계산한다.
    fun updateSession(command: SessionUpdateCommand) {
        val session =
            sessionPersistencePort.findSessionByIdForUpdate(command.sessionId.value)
                ?: throw SessionNotFoundException()

        sessionValidator.validateAttendanceTimes(command.attendanceStart, command.lateStart, command.absentStart)

        val previousAttendancePolicy = session.attendancePolicy

        session.updateSession(command)
        sessionPersistencePort.save(session)

        sessionFeedbackFormCommandService.applyOnSessionUpdate(
            sessionId = command.sessionId,
            feedbackEnabled = command.feedbackEnabled,
            feedbackStartAt = command.feedbackStartAt,
            feedbackPushEnabled = command.feedbackPushEnabled,
        )

        val attendanceTimesChanged =
            previousAttendancePolicy.hasChangedComparedTo(
                attendanceStart = command.attendanceStart,
                lateStart = command.lateStart,
                absentStart = command.absentStart,
            )
        if (!attendanceTimesChanged) return

        attendanceCommandService.applySessionPolicyChange(session, clock.instant())

        eventPublisher.publishEvent(
            SessionUpdateEvent(
                sessionId = session.id ?: throw InvalidSessionIdException(),
                lateStart = command.lateStart,
                absentStart = command.absentStart,
            ),
        )
    }

    // 세션 잠금을 잡고 함께 삭제해 진행 중인 인증/운영진 변경/자동 결석이 삭제된 기록을 되살리지 않게 한다.
    fun softDeleteSession(sessionId: SessionId) {
        val session =
            sessionPersistencePort.findSessionByIdForUpdate(sessionId.value)
                ?: throw SessionNotFoundException()

        val now = clock.instant()

        session.delete(now)
        sessionPersistencePort.save(session)
        attendanceCommandService.deleteAttendancesBySessionId(sessionId, now)

        eventPublisher.publishEvent(SessionDeleteEvent(sessionId, now))
    }

    private fun resolveAttendanceTimes(command: SessionCreateCommand): SessionAttendanceTimes {
        val attendanceStart = command.attendanceStart
        val lateStart = command.lateStart
        val absentStart = command.absentStart

        if (attendanceStart == null && lateStart == null && absentStart == null) {
            return attendancePolicyProperties.defaultOffsets.resolveFor(command.date)
        }
        if (attendanceStart == null || lateStart == null || absentStart == null) {
            throw PartialAttendanceTimesException()
        }

        sessionValidator.validateAttendanceTimes(attendanceStart, lateStart, absentStart)
        return SessionAttendanceTimes(attendanceStart, lateStart, absentStart)
    }
}
