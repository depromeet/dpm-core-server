package core.domain.sessionFeedback.enums

import java.time.Instant

/**
 * 세션 피드백 수집 상태. 운영진 화면의 배지/탭 헤더에 사용된다.
 */
enum class SessionFeedbackStatus {
    /** `now < startAt` */
    SCHEDULED,

    /** `startAt <= now < endAt` */
    IN_PROGRESS,

    /** `endAt <= now` */
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
