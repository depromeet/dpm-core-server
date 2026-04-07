package core.domain.session.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllCohortSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    fun save(session: Session): Session

    fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session>
}
