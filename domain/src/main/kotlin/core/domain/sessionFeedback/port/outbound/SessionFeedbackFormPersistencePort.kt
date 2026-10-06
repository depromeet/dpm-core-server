package core.domain.sessionFeedback.port.outbound

import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import java.time.Instant

interface SessionFeedbackFormPersistencePort {
    fun save(form: SessionFeedbackForm): SessionFeedbackForm

    fun findBySessionId(sessionId: Long): SessionFeedbackForm?

    fun findAllBySessionIds(sessionIds: Collection<Long>): List<SessionFeedbackForm>

    /** 지정 시점이 수집 구간(`startAt <= now < endAt`) 안에 있는 설문을 `endAt` 오름차순으로 반환한다. */
    fun findAllInProgressAt(now: Instant): List<SessionFeedbackForm>

    /**
     * 수집이 시작됐고(`startAt <= now`) PUSH 발송이 켜져 있으며 아직 발송되지 않은 설문을
     * `startAt` 오름차순으로 반환한다. 스케줄러에서 중복 발송 없이 1회만 보낼 대상 조회에 쓴다.
     */
    fun findAllPendingPushAt(now: Instant): List<SessionFeedbackForm>

    fun delete(form: SessionFeedbackForm)
}
