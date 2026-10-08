package core.persistence.sessionFeedback.repository

import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import core.entity.sessionFeedback.SessionFeedbackEntity
import org.springframework.stereotype.Repository

@Repository
class SessionFeedbackRepository(
    private val jpaRepository: SessionFeedbackJpaRepository,
) : SessionFeedbackPersistencePort {
    override fun save(feedback: SessionFeedback): SessionFeedback =
        jpaRepository.saveAndFlush(SessionFeedbackEntity.from(feedback)).toDomain()

    override fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): SessionFeedback? = jpaRepository.findBySessionIdAndMemberId(sessionId, memberId)?.toDomain()

    override fun existsBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): Boolean = jpaRepository.existsBySessionIdAndMemberId(sessionId, memberId)

    override fun findAllBySessionId(sessionId: Long): List<SessionFeedback> =
        jpaRepository.findAllBySessionId(sessionId).map { it.toDomain() }

    override fun countBySessionId(sessionId: Long): Long = jpaRepository.countBySessionId(sessionId)
}
