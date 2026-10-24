package core.application.sessionFeedback.application.scheduler

import core.application.support.AttendanceTestFixture
import core.application.support.FakeSessionFeedbackPushReceiptPersistencePort
import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.sessionFeedback.aggregate.SessionFeedbackForm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant

class SessionFeedbackReminderSchedulerTest {
    private val start = Instant.parse("2026-10-10T10:00:00Z")
    private val fixture = AttendanceTestFixture(now = start)
    private val receipts = FakeSessionFeedbackPushReceiptPersistencePort()
    private val notifications = RecordingNotificationCommandUseCase()
    private val transactionTemplate =
        TransactionTemplate(
            mock(PlatformTransactionManager::class.java).also { tm ->
                given(tm.getTransaction(any())).willReturn(SimpleTransactionStatus())
            },
        )
    private val scheduler =
        SessionFeedbackReminderScheduler(
            feedbackFormPersistencePort = fixture.feedbackForms,
            pushReceiptPersistencePort = receipts,
            sessionPersistencePort = fixture.sessions,
            attendancePersistencePort = fixture.attendances,
            notificationCommandUseCase = notifications,
            transactionTemplate = transactionTemplate,
            clock = fixture.clock,
        )

    @Test
    fun `수집 기간 중 아직 푸시를 받지 않은 출석·지각자에게만 보내고 마감 후에는 보내지 않는다`() {
        val cohortId = fixture.createActiveCohort()
        val session =
            fixture.createSession(
                cohortId,
                SessionAttendanceTimes(start, start.plusSeconds(300), start.plusSeconds(1800)),
            )
        fixture.feedbackForms.save(
            SessionFeedbackForm.create(session.id!!, start, pushEnabled = true),
        )
        fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PRESENT, attendedAt = start)
        fixture.addAttendance(session, memberId = 2L, status = AttendanceStatus.PENDING)

        scheduler.sendFeedbackOpenedPush()

        assertThat(notifications.sent).containsExactly(listOf(MemberId(1L)))
        assertThat(receipts.findMemberIdsBySessionId(session.id!!.value)).containsExactly(MemberId(1L))
        assertThat(fixture.feedbackForms.findBySessionId(session.id!!.value)!!.pushSentAt).isEqualTo(start)

        // 이후 출석한 사람만 추가 발송, 이미 받은 사람은 제외
        fixture.clock.now = start.plus(Duration.ofMinutes(30))
        fixture.addAttendance(session, memberId = 3L, status = AttendanceStatus.LATE, attendedAt = fixture.clock.now)
        notifications.sent.clear()

        scheduler.sendFeedbackOpenedPush()

        assertThat(notifications.sent).containsExactly(listOf(MemberId(3L)))
        assertThat(receipts.findMemberIdsBySessionId(session.id!!.value))
            .containsExactlyInAnyOrder(MemberId(1L), MemberId(3L))

        // 마감 이후에는 새 출석자도 대상이 아님
        fixture.clock.now = start.plus(Duration.ofHours(73))
        fixture.addAttendance(session, memberId = 4L, status = AttendanceStatus.PRESENT, attendedAt = fixture.clock.now)
        notifications.sent.clear()

        scheduler.sendFeedbackOpenedPush()

        assertThat(notifications.sent).isEmpty()
    }

    @Test
    fun `이미 one-shot 발송된 폼은 기존 출석자에게 재발송하지 않고 이후 출석자만 보낸다`() {
        val cohortId = fixture.createActiveCohort()
        val session =
            fixture.createSession(
                cohortId,
                SessionAttendanceTimes(start, start.plusSeconds(300), start.plusSeconds(1800)),
            )
        val form = SessionFeedbackForm.create(session.id!!, start, pushEnabled = true)
        form.markPushSent(start)
        fixture.feedbackForms.save(form)
        fixture.addAttendance(session, memberId = 1L, status = AttendanceStatus.PRESENT, attendedAt = start)
        fixture.addAttendance(
            session,
            memberId = 2L,
            status = AttendanceStatus.LATE,
            attendedAt = start.plus(Duration.ofMinutes(20)),
        )

        fixture.clock.now = start.plus(Duration.ofMinutes(30))
        scheduler.sendFeedbackOpenedPush()

        assertThat(notifications.sent).containsExactly(listOf(MemberId(2L)))
        assertThat(receipts.findMemberIdsBySessionId(session.id!!.value))
            .containsExactlyInAnyOrder(MemberId(1L), MemberId(2L))
    }

    private class RecordingNotificationCommandUseCase : NotificationCommandUseCase {
        val sent = mutableListOf<List<MemberId>>()

        override fun registerPushToken(
            memberId: MemberId,
            token: String,
        ): NotificationToken = error("unused")

        override fun deletePushToken(
            memberId: MemberId,
            token: String,
        ) = error("unused")

        override fun deleteAllPushTokens(memberId: MemberId) = error("unused")

        override fun sendCustomPushNotificationToMembers(
            memberIds: List<MemberId>,
            title: String,
            body: String,
            data: Map<String, Any>?,
        ) = error("unused")

        override fun sendPushNotificationToMembers(
            memberIds: List<MemberId>,
            messageType: NotificationMessageType,
            variables: Map<String, Any>,
            data: Map<String, Any>?,
        ) {
            assertThat(messageType).isEqualTo(NotificationMessageType.SESSION_FEEDBACK_OPENED)
            sent += memberIds
        }
    }
}
