package core.domain.sessionFeedback.enums

import java.time.Instant

enum class SessionFeedbackStatus {
        SCHEDULED,

        IN_PROGRESS,

        CLOSED,
    ;

    companion object {
        fun of(
            startAt: Instant,
            endAt: Instant,
            now: Instant,
        ): SessionFeedbackStatus =
            when {
                now.isBefore(startAt) -> SCHEDULED
                now.isBefore(endAt) -> IN_PROGRESS
                else -> CLOSED
            }
    }
}
