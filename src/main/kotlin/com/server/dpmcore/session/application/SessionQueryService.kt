package com.server.dpmcore.session.application

import com.server.dpmcore.cohort.application.CohortQueryService
import com.server.dpmcore.session.application.query.SessionWeekQueryModel
import com.server.dpmcore.session.domain.exception.SessionNotFoundException
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class SessionQueryService(
    private val cohortQueryService: CohortQueryService,
    private val sessionPersistencePort: SessionPersistencePort,
) {
    fun getNextSession(): Session? {
        val koreaZone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.ofInstant(Instant.now(), koreaZone)
        val startOfToday = today.atStartOfDay(koreaZone).toInstant()

        return sessionPersistencePort.findNextSessionBy(startOfToday)
    }

    fun getAllSessions(): List<Session> {
        val cohortId = cohortQueryService.getLatestCohortId()

        return sessionPersistencePort.findAllSessions(cohortId)
    }

    fun getSessionById(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionById(sessionId)
            ?: throw SessionNotFoundException()

    fun getAttendanceTime(sessionId: SessionId): Instant =
        sessionPersistencePort
            .findSessionById(sessionId)
            ?.attendancePolicy
            ?.attendanceStart
            ?: throw SessionNotFoundException()

    fun getSessionWeeks(): List<SessionWeekQueryModel> {
        val cohortId = cohortQueryService.getLatestCohortId()

        return sessionPersistencePort
            .findAllSessions(cohortId)
            .map { SessionWeekQueryModel(sessionId = it.id!!, week = it.week) }
    }
}
