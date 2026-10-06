package core.application.sessionFeedback.presentation.response

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import java.time.Clock
import java.time.LocalDateTime

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
