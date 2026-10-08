package core.persistence.sessionFeedback.repository

import core.entity.sessionFeedback.SessionFeedbackFormEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SessionFeedbackFormJpaRepository : JpaRepository<SessionFeedbackFormEntity, Long> {
    fun findBySessionIdAndDeletedAtIsNull(sessionId: Long): SessionFeedbackFormEntity?

    fun findAllBySessionIdInAndDeletedAtIsNull(sessionIds: Collection<Long>): List<SessionFeedbackFormEntity>

    fun findAllByStartAtLessThanEqualAndEndAtGreaterThanAndDeletedAtIsNullOrderByEndAtAsc(
        startAt: Instant,
        endAt: Instant,
    ): List<SessionFeedbackFormEntity>

    fun findAllByStartAtLessThanEqualAndPushEnabledIsTrueAndPushSentAtIsNullAndDeletedAtIsNullOrderByStartAtAsc(
        startAt: Instant,
    ): List<SessionFeedbackFormEntity>
}
