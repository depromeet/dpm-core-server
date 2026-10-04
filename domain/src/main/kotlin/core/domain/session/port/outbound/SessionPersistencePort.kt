package core.domain.session.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllCohortSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    /** FOR UPDATE. 세션 시각 변경/삭제/운영진 출석 변경을 직렬화한다. */
    fun findSessionByIdForUpdate(sessionId: Long): Session?

    /** FOR SHARE. 인증끼리는 동시에 진행하고 쓰기 잠금 작업과는 직렬화된다. */
    fun findSessionByIdForShare(sessionId: Long): Session?

    fun save(session: Session): Session

    fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session>

}
