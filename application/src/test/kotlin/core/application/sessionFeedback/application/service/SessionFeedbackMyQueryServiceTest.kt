package core.application.sessionFeedback.application.service

import core.application.support.AttendanceTestFixture
import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SessionFeedbackMyQueryServiceTest {
    private val start = Instant.parse("2026-10-10T10:00:00Z")
    private val fixture = AttendanceTestFixture(now = start.plus(Duration.ofHours(1)))
    private val feedbacks = InMemorySessionFeedbackPersistencePort()
    private val service =
        SessionFeedbackMyQueryService(
            sessionPersistencePort = fixture.sessions,
            feedbackFormPersistencePort = fixture.feedbackForms,
            feedbackPersistencePort = feedbacks,
            attendancePersistencePort = fixture.attendances,
            clock = fixture.clock,
        )

    @Test
    fun `출석 명단에 있으나 PENDING 이면 ATTENDANCE_INCOMPLETE 이고 출석·지각만 AVAILABLE 이다`() {
        val cohortId = fixture.createActiveCohort()
        val session =
            fixture.createSession(
                cohortId,
                SessionAttendanceTimes(start, start.plusSeconds(300), start.plusSeconds(1800)),
            )
        fixture.feedbackForms.save(SessionFeedbackForm.create(session.id!!, start, pushEnabled = true))
        fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PENDING)
        fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.PRESENT, attendedAt = start)
        fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.ABSENT)

        assertThat(service.getMyFeedback(session.id!!, MemberId(1L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.ATTENDANCE_INCOMPLETE)
        assertThat(service.getMyFeedback(session.id!!, MemberId(2L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.AVAILABLE)
        assertThat(service.getMyFeedback(session.id!!, MemberId(3L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.NOT_TARGET)
        assertThat(service.getMyFeedback(session.id!!, MemberId(99L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.NOT_TARGET)
    }

    private class InMemorySessionFeedbackPersistencePort : SessionFeedbackPersistencePort {
        private val rows = mutableListOf<SessionFeedback>()

        override fun save(feedback: SessionFeedback): SessionFeedback = feedback.also { rows += it }

        override fun findBySessionIdAndMemberId(
            sessionId: Long,
            memberId: Long,
        ): SessionFeedback? = rows.firstOrNull { it.sessionId.value == sessionId && it.memberId.value == memberId }

        override fun existsBySessionIdAndMemberId(
            sessionId: Long,
            memberId: Long,
        ): Boolean = findBySessionIdAndMemberId(sessionId, memberId) != null

        override fun findAllBySessionId(sessionId: Long): List<SessionFeedback> =
            rows.filter { it.sessionId.value == sessionId }

        override fun countBySessionId(sessionId: Long): Long = findAllBySessionId(sessionId).size.toLong()
    }
}
