package core.domain.sessionFeedback.port.outbound

import core.domain.sessionFeedback.aggregate.SessionFeedbackForm

interface SessionFeedbackFormPersistencePort {
    fun save(form: SessionFeedbackForm): SessionFeedbackForm

    fun findBySessionId(sessionId: Long): SessionFeedbackForm?

    fun findAllBySessionIds(sessionIds: Collection<Long>): List<SessionFeedbackForm>

    fun delete(form: SessionFeedbackForm)
}
