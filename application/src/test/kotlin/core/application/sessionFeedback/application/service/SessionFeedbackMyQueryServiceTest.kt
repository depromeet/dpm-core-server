package core.application.sessionFeedback.application.service

import core.application.support.AttendanceTestFixture
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackMyStatus
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
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
    fun `미출석은 ATTENDANCE_INCOMPLETE 이고 미등록 결석과 인정결석은 NOT_TARGET 이다`() {
        val cohortId = fixture.createActiveCohort()
        val session =
            fixture.createSession(
                cohortId,
                SessionAttendanceTimes(start, start.plusSeconds(300), start.plusSeconds(1800)),
            )
        fixture.feedbackForms.save(SessionFeedbackForm.create(session.id!!, start, pushEnabled = true))
        fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PENDING)
        fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.PRESENT, attendedAt = start)
        fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.ABSENT, attendedAt = start)
        fixture.addAttendance(session, memberId = 4L, status = AttendanceStatus.EXCUSED_ABSENT)

        assertThat(service.getMyFeedback(session.id!!, MemberId(1L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.ATTENDANCE_INCOMPLETE)
        assertThat(service.getMyFeedback(session.id!!, MemberId(2L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.AVAILABLE)
        assertThat(service.getMyFeedback(session.id!!, MemberId(3L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.NOT_TARGET)
        assertThat(service.getMyFeedback(session.id!!, MemberId(4L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.NOT_TARGET)
        assertThat(service.getMyFeedback(session.id!!, MemberId(99L)).myStatus)
            .isEqualTo(SessionFeedbackMyStatus.NOT_TARGET)
    }

    @Test
    fun `늦은 참여 등록자는 목록과 배너에서 진입해 피드백을 제출하고 응답률에 포함된다`() {
        val session =
            fixture.createSession(
                fixture.createActiveCohort(),
                SessionAttendanceTimes(start, start.plusSeconds(300), start.plusSeconds(1800)),
            )
        val id = requireNotNull(session.id)
        val member = MemberId(1L)
        fixture.feedbackForms.save(SessionFeedbackForm.create(id, start, pushEnabled = true))
        fixture.attendances.save(
            Attendance(
                sessionId = id,
                memberId = member,
                status = AttendanceStatus.ABSENT,
                attendedAt = fixture.clock.instant(),
                lateParticipatedAt = fixture.clock.instant(),
            ),
        )
        fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.ABSENT, attendedAt = start)
        val list =
            SessionFeedbackListQueryService(
                fixture.feedbackForms,
                feedbacks,
                fixture.attendances,
                fixture.clock,
            )
        val pending =
            SessionFeedbackPendingQueryService(
                fixture.sessions,
                fixture.feedbackForms,
                feedbacks,
                fixture.attendances,
                fixture.clock,
            )
        val submit =
            SessionFeedbackCommandService(
                fixture.sessions,
                fixture.feedbackForms,
                feedbacks,
                fixture.attendances,
                fixture.clock,
            )
        val insight =
            SessionFeedbackInsightQueryService(
                fixture.sessions,
                fixture.feedbackForms,
                feedbacks,
                fixture.attendances,
                fixture.clock,
            )

        assertThat(service.getMyFeedback(id, member).myStatus).isEqualTo(SessionFeedbackMyStatus.AVAILABLE)
        assertThat(list.buildFor(listOf(session), member).getValue(id.value).canSubmit).isTrue()
        assertThat(pending.findPending(member)?.sessionId).isEqualTo(id.value)
        submit.submit(
            sessionId = id,
            memberId = member,
            satisfaction = SessionFeedbackSatisfaction.SATISFIED,
            likedAspects = listOf(SessionFeedbackAspect.NOTHING),
            improvementAspects = listOf(SessionFeedbackAspect.NOTHING),
            likedEtc = null,
            improvementEtc = null,
            freeComment = null,
        )

        assertThat(service.getMyFeedback(id, member).myStatus).isEqualTo(SessionFeedbackMyStatus.SUBMITTED)
        assertThat(list.buildFor(listOf(session), member).getValue(id.value).canSubmit).isFalse()
        assertThat(pending.findPending(member)).isNull()
        val summary = insight.getInsight(id).responseSummary
        assertThat(summary.respondentCount).isEqualTo(1)
        assertThat(summary.targetCount).isEqualTo(1)
        assertThat(summary.responseRate).isEqualTo(100)
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

        override fun findAllBySessionId(sessionId: Long): List<SessionFeedback> = rows.filter { it.sessionId.value == sessionId }

        override fun countBySessionId(sessionId: Long): Long = findAllBySessionId(sessionId).size.toLong()
    }
}
