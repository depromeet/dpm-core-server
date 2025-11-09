package core.application.session.application.service

import core.application.session.application.exception.SessionNotFoundException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.inbound.query.SessionWeekQueryModel
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class SessionQueryService(
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val sessionPersistencePort: SessionPersistencePort,
) {
    fun getNextSession(): Session? {
        val koreaZone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.ofInstant(Instant.now(), koreaZone)
        val startOfToday = today.atStartOfDay(koreaZone).toInstant()

        return sessionPersistencePort.findNextSessionBy(startOfToday)
    }

    fun getAllSessions(): List<Session> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort.findAllSessions(cohortId.value)
    }

    fun getSessionById(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionById(sessionId.value)
            ?: throw SessionNotFoundException()

    fun getAttendanceTime(sessionId: SessionId): Instant =
        sessionPersistencePort
            .findSessionById(sessionId.value)
            ?.attendancePolicy
            ?.attendanceStart
            ?: throw SessionNotFoundException()

    fun getSessionWeeks(): List<SessionWeekQueryModel> {
        val cohortId = cohortQueryUseCase.getLatestCohortId()

        return sessionPersistencePort
            .findAllSessions(cohortId.value)
            .map { SessionWeekQueryModel(sessionId = it.id!!, week = it.week, date = it.date) }
    }
}
