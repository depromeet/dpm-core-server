package core.persistence.sessionFeedback.repository

import core.entity.sessionFeedback.SessionFeedbackEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SessionFeedbackJpaRepository : JpaRepository<SessionFeedbackEntity, Long> {
    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): SessionFeedbackEntity?

    fun existsBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): Boolean

    fun findAllBySessionId(sessionId: Long): List<SessionFeedbackEntity>

    fun countBySessionId(sessionId: Long): Long
}
