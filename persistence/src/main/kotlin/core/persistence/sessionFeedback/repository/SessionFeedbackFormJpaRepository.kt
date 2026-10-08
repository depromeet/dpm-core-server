package core.persistence.sessionFeedback.repository

import core.entity.sessionFeedback.SessionFeedbackFormEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SessionFeedbackFormJpaRepository : JpaRepository<SessionFeedbackFormEntity, Long> {
    fun findBySessionIdAndDeletedAtIsNull(sessionId: Long): SessionFeedbackFormEntity?

    fun findAllBySessionIdInAndDeletedAtIsNull(sessionIds: Collection<Long>): List<SessionFeedbackFormEntity>
}
