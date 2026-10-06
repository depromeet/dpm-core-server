package core.application.sessionFeedback.application.service

import core.application.support.FakeSessionFeedbackFormPersistencePort
import core.application.support.MutableClock
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SessionFeedbackFormCommandServiceTest {
    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val forms = FakeSessionFeedbackFormPersistencePort()
    private val service = SessionFeedbackFormCommandService(forms, MutableClock(now))
    private val sessionId = SessionId(1L)

    @Test
    fun `시작 전에 피드백을 껐다가 다시 켜면 새 시작 시각으로 폼이 하나만 남는다`() {
        service.applyOnSessionCreate(sessionId, true, now.plus(Duration.ofHours(1)), true)

        service.applyOnSessionUpdate(sessionId, false, null, true)
        assertThat(forms.all()).isEmpty()

        val restartAt = now.plus(Duration.ofHours(5))
        service.applyOnSessionUpdate(sessionId, true, restartAt, false)

        val form = forms.all().single()
        assertThat(form.startAt).isEqualTo(restartAt)
        assertThat(form.endAt).isEqualTo(restartAt.plus(Duration.ofHours(72)))
        assertThat(form.pushEnabled).isFalse()
    }
}
