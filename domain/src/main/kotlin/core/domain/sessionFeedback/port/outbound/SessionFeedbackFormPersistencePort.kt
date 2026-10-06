package core.domain.sessionFeedback.port.outbound

import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import java.time.Instant

interface SessionFeedbackFormPersistencePort {
    fun save(form: SessionFeedbackForm): SessionFeedbackForm

    fun findBySessionId(sessionId: Long): SessionFeedbackForm?

    fun findAllBySessionIds(sessionIds: Collection<Long>): List<SessionFeedbackForm>

    /** 지정 시점이 수집 구간(`startAt <= now < endAt`) 안에 있는 설문을 `endAt` 오름차순으로 반환한다. */
    fun findAllInProgressAt(now: Instant): List<SessionFeedbackForm>

    fun delete(form: SessionFeedbackForm)
}
