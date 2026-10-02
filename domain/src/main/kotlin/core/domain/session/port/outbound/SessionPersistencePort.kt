package core.domain.session.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionId
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllCohortSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    /** FOR UPDATE. 세션 시각 변경/삭제/운영진 출석 변경을 직렬화한다. */
    fun findSessionByIdForUpdate(sessionId: Long): Session?

    /** FOR SHARE. 인증과 자동 결석끼리는 동시에 진행하고 쓰기 잠금 작업과는 직렬화된다. */
    fun findSessionByIdForShare(sessionId: Long): Session?

    fun save(session: Session): Session

    fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session>

    /**
     * 모든 기수에서 자동 결석 후보 세션 ID 를 조회합니다.
     * 삭제되지 않았고, 인증 마감이 [absentStartTo] 이하(과거 기수 포함, 하한 없음)이며,
     * 자동 결석 대상 출석 행이 하나 이상 남은 세션만 반환합니다.
     */
    fun findSessionIdsToAutoClose(absentStartTo: Instant): List<SessionId>
}
