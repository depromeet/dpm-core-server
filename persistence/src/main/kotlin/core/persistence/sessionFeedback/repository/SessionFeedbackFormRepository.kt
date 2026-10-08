package core.persistence.sessionFeedback.repository

import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.entity.sessionFeedback.SessionFeedbackFormEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class SessionFeedbackFormRepository(
    private val jpaRepository: SessionFeedbackFormJpaRepository,
) : SessionFeedbackFormPersistencePort {
    override fun save(form: SessionFeedbackForm): SessionFeedbackForm =
        jpaRepository.save(SessionFeedbackFormEntity.from(form)).toDomain()

    override fun findBySessionId(sessionId: Long): SessionFeedbackForm? =
        jpaRepository.findBySessionIdAndDeletedAtIsNull(sessionId)?.toDomain()

    override fun findAllBySessionIds(sessionIds: Collection<Long>): List<SessionFeedbackForm> {
        if (sessionIds.isEmpty()) return emptyList()
        return jpaRepository.findAllBySessionIdInAndDeletedAtIsNull(sessionIds).map { it.toDomain() }
    }

    override fun findAllInProgressAt(now: Instant): List<SessionFeedbackForm> =
        jpaRepository
            .findAllByStartAtLessThanEqualAndEndAtGreaterThanAndDeletedAtIsNullOrderByEndAtAsc(now, now)
            .map { it.toDomain() }

    override fun findAllPendingPushAt(now: Instant): List<SessionFeedbackForm> =
        jpaRepository
            .findAllByStartAtLessThanEqualAndPushEnabledIsTrueAndPushSentAtIsNullAndDeletedAtIsNullOrderByStartAtAsc(now)
            .map { it.toDomain() }

    override fun delete(form: SessionFeedbackForm) {
        val id = form.id?.value ?: return
        jpaRepository.deleteById(id)
    }
}
