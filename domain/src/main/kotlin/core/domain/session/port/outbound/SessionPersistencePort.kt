package core.domain.session.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import java.time.Instant

interface SessionPersistencePort {
    fun findNextSessionBy(startOfToday: Instant): Session?

    fun findAllCohortSessions(cohortId: Long): List<Session>

    fun findSessionById(sessionId: Long): Session?

    /**
     * 삭제되지 않은 세션을 쓰기 잠금(SELECT ... FOR UPDATE)으로 조회합니다.
     * 세션 시각 변경/삭제/정책 재계산/운영진 출석 변경에서 사용합니다. 트랜잭션 안에서 호출해야 합니다.
     */
    fun findSessionByIdForUpdate(sessionId: Long): Session?

    /**
     * 삭제되지 않은 세션을 공유 잠금(SELECT ... FOR SHARE)으로 조회합니다.
     * 출석 인증에서 사용합니다. 쓰기 잠금을 쓰는 작업과는 직렬화되고, 서로는 동시에 진행됩니다.
     */
    fun findSessionByIdForShare(sessionId: Long): Session?

    fun save(session: Session): Session

    fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session>

}
