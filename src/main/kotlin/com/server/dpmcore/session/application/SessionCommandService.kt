package com.server.dpmcore.session.application

import com.server.dpmcore.session.application.config.SessionAttendanceProperties
import com.server.dpmcore.session.domain.exception.SessionNotFoundException
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.domain.port.inbound.command.SessionCreateCommand
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class SessionCommandService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val sessionAttendanceProperties: SessionAttendanceProperties,
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

        sessionPersistencePort.save(newSession)

        // TODO: eventPublisher로 세션 생성 이벤트 발행 - 출석부 생성 등 처리
    }
}
