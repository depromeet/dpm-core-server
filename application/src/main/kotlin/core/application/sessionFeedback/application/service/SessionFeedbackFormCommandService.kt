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

@Service
@Transactional
class SessionFeedbackFormCommandService(
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val clock: Clock,
) {
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
        // session_id 유니크 제약 때문에 soft delete 하면 다시 켤 때 INSERT 가 실패한다. 시작 전이라 응답도 없다.
        feedbackFormPersistencePort.delete(existing)
    }

    private fun requireStartAt(startAt: Instant?): Instant = startAt ?: throw FeedbackStartAtRequiredException()

    private fun validateFutureStartAt(
        startAt: Instant,
        now: Instant,
    ) {
        if (!startAt.isAfter(now)) throw InvalidFeedbackStartAtException()
    }
}
