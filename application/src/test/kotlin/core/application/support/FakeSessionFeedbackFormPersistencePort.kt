package core.application.support

import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.vo.SessionFeedbackFormId
import java.util.concurrent.atomic.AtomicLong

class FakeSessionFeedbackFormPersistencePort : SessionFeedbackFormPersistencePort {
    private val sequence = AtomicLong(0)
    private val rows = linkedMapOf<Long, SessionFeedbackForm>()

    @Synchronized
    override fun save(form: SessionFeedbackForm): SessionFeedbackForm {
        val id = form.id?.value ?: sequence.incrementAndGet()
        val persisted =
            SessionFeedbackForm(
                id = SessionFeedbackFormId(id),
                sessionId = form.sessionId,
                startAt = form.startAt,
                endAt = form.endAt,
                pushEnabled = form.pushEnabled,
                pushSentAt = form.pushSentAt,
                createdAt = form.createdAt,
                updatedAt = form.updatedAt,
                deletedAt = form.deletedAt,
            )
        rows[id] = persisted
        return persisted
    }

    @Synchronized
    override fun findBySessionId(sessionId: Long): SessionFeedbackForm? =
        rows.values.firstOrNull { it.sessionId == SessionId(sessionId) && it.deletedAt == null }

    @Synchronized
    override fun findAllBySessionIds(sessionIds: Collection<Long>): List<SessionFeedbackForm> {
        if (sessionIds.isEmpty()) return emptyList()
        val set = sessionIds.toSet()
        return rows.values.filter { it.sessionId.value in set && it.deletedAt == null }
    }

    @Synchronized
    override fun delete(form: SessionFeedbackForm) {
        val id = form.id?.value ?: return
        rows.remove(id)
    }

    @Synchronized
    fun all(): List<SessionFeedbackForm> = rows.values.toList()
}
