package core.application.sessionFeedback.application.service

import core.application.sessionFeedback.application.exception.FeedbackAlreadyStartedException
import core.application.sessionFeedback.application.exception.FeedbackStartAtRequiredException
import core.application.sessionFeedback.application.exception.InvalidFeedbackStartAtException
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 세션 생성·수정 시 피드백 설문 설정(session_feedback_forms) 을 upsert 한다.
 *
 * 세션 CRUD 흐름에서 세션이 저장된 뒤 호출된다. 호출 측은 분리해 두어 피드백 비활성 세션에선
 * 아무 쿼리도 돌지 않도록 한다.
 */
@Service
@Transactional
class SessionFeedbackFormCommandService(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val clock: Clock,
) {
    /** 세션 생성 시 호출. feedbackEnabled 가 false 면 아무것도 하지 않는다. */
    fun applyOnSessionCreate(
        sessionId: SessionId,
        feedbackEnabled: Boolean,
        feedbackStartAt: Instant?,
        feedbackPushEnabled: Boolean,
    ) {
        if (!feedbackEnabled) return

        val startAt = requireStartAt(feedbackStartAt)
        validateFutureStartAt(startAt, clock.instant())

        feedbackFormPersistencePort.save(
            SessionFeedbackForm.create(
                sessionId = sessionId,
                startAt = startAt,
                pushEnabled = feedbackPushEnabled,
            ),
        )
    }

    /**
     * 세션 수정 시 호출. 다음 규칙으로 설문을 생성/수정/비활성화 한다.
     *
     * - 기존 설문 없음 + enabled=false → no-op
     * - 기존 설문 없음 + enabled=true → 신규 생성 (startAt > now 검증)
     * - 기존 설문 있음 + enabled=false → 수집 시작 전이면 soft delete, 시작 후면 409-02
     * - 기존 설문 있음 + enabled=true + startAt 동일 → push 만 갱신 (startAt 검증 생략)
     * - 기존 설문 있음 + enabled=true + startAt 변경 → 수집 시작 전이면 schedule 갱신, 시작 후면 409-02
     */
    fun applyOnSessionUpdate(
        sessionId: SessionId,
        feedbackEnabled: Boolean,
        feedbackStartAt: Instant?,
        feedbackPushEnabled: Boolean,
    ) {
        val now = clock.instant()
        val existing = feedbackFormPersistencePort.findBySessionId(sessionId.value)

        if (!feedbackEnabled) {
            disable(existing, now)
            return
        }

        val requestedStartAt = requireStartAt(feedbackStartAt)

        if (existing == null) {
            validateFutureStartAt(requestedStartAt, now)
            feedbackFormPersistencePort.save(
                SessionFeedbackForm.create(sessionId, requestedStartAt, feedbackPushEnabled),
            )
            return
        }

        if (existing.startAt == requestedStartAt) {
            existing.updatePushEnabled(feedbackPushEnabled)
            feedbackFormPersistencePort.save(existing)
            return
        }

        if (existing.isCollectionStarted(now)) throw FeedbackAlreadyStartedException()
        validateFutureStartAt(requestedStartAt, now)
        existing.updateSchedule(requestedStartAt, feedbackPushEnabled)
        feedbackFormPersistencePort.save(existing)
    }

    private fun disable(
        existing: SessionFeedbackForm?,
        now: Instant,
    ) {
        if (existing == null) return
        if (existing.isCollectionStarted(now)) throw FeedbackAlreadyStartedException()
        existing.delete(now)
        feedbackFormPersistencePort.save(existing)
    }

    private fun requireStartAt(startAt: Instant?): Instant = startAt ?: throw FeedbackStartAtRequiredException()

    private fun validateFutureStartAt(
        startAt: Instant,
        now: Instant,
    ) {
        if (!startAt.isAfter(now)) throw InvalidFeedbackStartAtException()
    }
}
