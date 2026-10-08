package core.domain.sessionFeedback.port.outbound

import core.domain.sessionFeedback.aggregate.SessionFeedback

interface SessionFeedbackPersistencePort {
    fun save(feedback: SessionFeedback): SessionFeedback

    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): SessionFeedback?

    fun existsBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): Boolean

    fun findAllBySessionId(sessionId: Long): List<SessionFeedback>

    fun countBySessionId(sessionId: Long): Long
}
