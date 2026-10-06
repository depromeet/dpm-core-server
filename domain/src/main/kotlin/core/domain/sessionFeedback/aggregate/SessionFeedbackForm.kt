package core.domain.sessionFeedback.aggregate

import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import core.domain.sessionFeedback.vo.SessionFeedbackFormId
import java.time.Duration
import java.time.Instant

/**
 * 세션 피드백 설문 설정(SessionFeedbackForm) 도메인 모델.
 *
 * `피드백 받기` ON 세션에만 생성된다. `endAt` 은 서버가 `startAt + 72h` 로 계산하며 직접 수정하지 않는다.
 */
class SessionFeedbackForm(
    val id: SessionFeedbackFormId? = null,
    val sessionId: SessionId,
    startAt: Instant,
    endAt: Instant,
    pushEnabled: Boolean,
    pushSentAt: Instant? = null,
    createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var startAt: Instant = startAt
        private set

    var endAt: Instant = endAt
        private set

    var pushEnabled: Boolean = pushEnabled
        private set

    var pushSentAt: Instant? = pushSentAt
        private set

    var createdAt: Instant? = createdAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    /** 수집 시작 시각과 PUSH 설정을 함께 변경한다. `endAt` 은 재계산된다. */
    fun updateSchedule(
        startAt: Instant,
        pushEnabled: Boolean,
    ) {
        this.startAt = startAt
        this.endAt = startAt.plus(COLLECT_WINDOW)
        this.pushEnabled = pushEnabled
        this.updatedAt = Instant.now()
    }

    /** PUSH 설정만 변경한다. 수집이 시작된 뒤에도 호출 가능. */
    fun updatePushEnabled(pushEnabled: Boolean) {
        if (this.pushEnabled == pushEnabled) return
        this.pushEnabled = pushEnabled
        this.updatedAt = Instant.now()
    }

    /** 수집 시작 PUSH 를 발송했음을 기록한다. 중복 발송 방지 용도. */
    fun markPushSent(at: Instant) {
        this.pushSentAt = at
        this.updatedAt = Instant.now()
    }

    fun delete(deletedAt: Instant = Instant.now()) {
        this.deletedAt = deletedAt
        this.updatedAt = deletedAt
    }

    fun statusAt(now: Instant): SessionFeedbackStatus = SessionFeedbackStatus.of(startAt, endAt, now)

    fun isCollectionStarted(now: Instant): Boolean = !now.isBefore(startAt)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionFeedbackForm) return false
        return id == other.id && sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + sessionId.hashCode()
        return result
    }

    override fun toString(): String =
        "SessionFeedbackForm(id=$id, sessionId=$sessionId, startAt=$startAt, endAt=$endAt, pushEnabled=$pushEnabled)"

    companion object {
        val COLLECT_WINDOW: Duration = Duration.ofHours(72)

        fun create(
            sessionId: SessionId,
            startAt: Instant,
            pushEnabled: Boolean,
        ): SessionFeedbackForm =
            SessionFeedbackForm(
                sessionId = sessionId,
                startAt = startAt,
                endAt = startAt.plus(COLLECT_WINDOW),
                pushEnabled = pushEnabled,
                createdAt = Instant.now(),
            )
    }
}
