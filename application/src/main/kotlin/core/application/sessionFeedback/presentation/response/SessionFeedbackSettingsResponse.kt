package core.application.sessionFeedback.presentation.response

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import java.time.Clock
import java.time.LocalDateTime

/**
 * 세션 상세 응답에 포함되는 피드백 설정 정보. 피드백 받기 OFF 세션에선 `feedback: null` 로 내려간다.
 */
data class SessionFeedbackSettingsResponse(
    val status: SessionFeedbackStatus,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val pushEnabled: Boolean,
) {
    companion object {
        fun of(
            form: SessionFeedbackForm,
            clock: Clock,
        ): SessionFeedbackSettingsResponse =
            SessionFeedbackSettingsResponse(
                status = form.statusAt(clock.instant()),
                startAt = instantToLocalDateTime(form.startAt),
                endAt = instantToLocalDateTime(form.endAt),
                pushEnabled = form.pushEnabled,
            )
    }
}
