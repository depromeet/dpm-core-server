package core.application.sessionFeedback.application.service

import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SessionFeedbackFormQueryService(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
) {
    fun findBySessionId(sessionId: SessionId): SessionFeedbackForm? =
        feedbackFormPersistencePort.findBySessionId(sessionId.value)

    fun findAllBySessionIds(sessionIds: Collection<Long>): Map<Long, SessionFeedbackForm> =
        feedbackFormPersistencePort
            .findAllBySessionIds(sessionIds)
            .associateBy { it.sessionId.value }
}
