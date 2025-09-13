package com.server.dpmcore.session.application

import com.server.dpmcore.session.application.exception.AttendanceStartTimeDateMismatchException
import com.server.dpmcore.session.application.exception.InvalidSessionIdException
import com.server.dpmcore.session.application.exception.SessionNotFoundException
import com.server.dpmcore.session.domain.event.SessionCreateEvent
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
            sessionPersistencePort.findSessionById(sessionId.value)
                ?: throw SessionNotFoundException()

        checkIsSameDateAsSession(session, attendanceStartTime)
        session.updateAttendanceStartTime(attendanceStartTime)

        sessionPersistencePort.save(session)
    }

    /**
     * 출석 시작 시간이 세션과 같은 날짜인지 확인합니다.
     *
     * @param session 세션
     * @param attendanceStartTime 출석 시작 시간
     * @throws AttendanceStartTimeDateMismatchException 날짜가 다를 경우
     * @author LeeHanEum
     * @since 2025.09.13
     */
    private fun checkIsSameDateAsSession(
        session: Session,
        attendanceStartTime: Instant,
    ) {
        if (!session.isSameDateAsSession(attendanceStartTime)) throw AttendanceStartTimeDateMismatchException()
    }

    fun createSession(command: SessionCreateCommand) {
        val newSession = Session.create(command)

        val savedSession = sessionPersistencePort.save(newSession)

        eventPublisher.publishEvent(SessionCreateEvent(savedSession.id ?: throw InvalidSessionIdException()))
    }
}
