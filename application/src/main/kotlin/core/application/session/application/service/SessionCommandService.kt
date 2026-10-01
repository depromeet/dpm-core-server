package core.application.session.application.service

import core.application.attendance.application.properties.AttendancePolicyProperties
import core.application.attendance.application.service.AttendanceCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.session.application.exception.InvalidSessionIdException
import core.application.session.application.exception.PartialAttendanceTimesException
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
 * 세션 생성/수정/삭제.
 *
 * 세션 행을 바꾸는 쓰기는 세션 행 쓰기 잠금을 먼저 잡고, 그 다음 출석 행을 갱신한다(잠금 순서: session -> attendance).
 * 인증/자동 결석은 세션 공유 잠금, 운영진 출석 변경은 세션 쓰기 잠금을 잡으므로 세션 시각 변경/삭제와 직렬화된다.
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
    private val clock: Clock,
) {
    /**
     * 인증 시작 시각만 변경한다. 출석/지각 판정 경계(지각 시작, 마감)는 그대로이므로 출석 기록은 재계산하지 않는다.
     * 세션 날짜와 다른 날이어도 된다(자정 직후 세션의 T-10 등). 순서(인증 시작 < 지각 시작 < 마감)만 검증한다.
     */
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

    /**
     * 세션을 수정한다. 출석 시각이 바뀌면 같은 트랜잭션에서 세션 잠금을 유지한 채 출석 기록을 재계산한다.
     * (비동기 이벤트의 오래된 시각 값이 이후 변경을 덮어쓰지 않도록)
     */
    fun updateSession(command: SessionUpdateCommand) {
        val session =
            sessionPersistencePort.findSessionByIdForUpdate(command.sessionId.value)
                ?: throw SessionNotFoundException()

        sessionValidator.validateAttendanceTimes(command.attendanceStart, command.lateStart, command.absentStart)

        val previousAttendancePolicy = session.attendancePolicy

        session.updateSession(command)
        sessionPersistencePort.save(session)

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

    /**
     * 세션과 출석 기록을 같은 트랜잭션에서 소프트 삭제한다.
     * 세션 잠금을 잡고 삭제하므로 동시에 진행 중인 인증/운영진 변경/자동 결석이 삭제된 기록을 되살리지 않는다.
     */
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

    /**
     * 출석 시각 세 개를 모두 생략하면 환경 설정 기본값(attendance.policy)으로 계산하고, 모두 제공하면 명시적인 세션별 예외로 사용한다.
     * 일부만 제공하면 거절한다.
     */
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
