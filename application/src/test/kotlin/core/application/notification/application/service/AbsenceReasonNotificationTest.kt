package core.application.notification.application.service

import com.sun.net.httpserver.HttpServer
import core.application.notification.application.event.AbsenceReasonNotificationListener
import core.domain.cohort.vo.CohortId
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.event.AbsenceReasonReviewedEvent
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import core.domain.notification.port.outbound.NotificationPersistencePort
import core.domain.session.vo.SessionId
import core.persistence.expo.ExpoPriority
import core.persistence.expo.ExpoPushClient
import core.persistence.expo.ExpoPushTicket
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class AbsenceReasonNotificationTest {
    private val staff = mock(MemberRolePersistencePort::class.java)
    private val tokens = mock(NotificationPersistencePort::class.java)
    private val expo = mock(ExpoPushClient::class.java)
    private val listener = AbsenceReasonNotificationListener(staff, AbsenceReasonPushService(tokens, expo))
    private val member = MemberId(123)
    private val session = SessionId(456)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `심사 결과는 제출자 기기에만 해당 결정과 세션 번호를 보낸다`(approved: Boolean) {
        val devices = listOf("device")
        given(tokens.findTokensForRecipients(listOf(member))).willReturn(devices)
        val type =
            if (approved) {
                NotificationMessageType.ABSENCE_REASON_APPROVED
            } else {
                NotificationMessageType.ABSENCE_REASON_REJECTED
            }

        listener.onReviewed(AbsenceReasonReviewedEvent(session, member, 2, approved))

        verify(expo).sendBatch(
            devices,
            type.title,
            type.format(mapOf("week" to 2)),
            mapOf("type" to type.name, "sessionId" to session.value),
            ExpoPriority.NORMAL,
            retryTransport = false,
        )
        verifyNoInteractions(staff)
    }

    @Test
    fun `제출 알림은 일시적으로 거절된 기기만 한 번 더 보내고 성공 영구오류 결과누락은 재전송하지 않는다`() {
        val cohort = CohortId(19)
        val reviewers = listOf(MemberId(2), MemberId(3))
        val devices = listOf("ok", "limited", "unregistered", "missing")
        given(staff.findAttendanceStaffIds(cohort)).willReturn(reviewers)
        given(tokens.findTokensForRecipients(reviewers)).willReturn(devices)
        val type = NotificationMessageType.ABSENCE_REASON_SUBMITTED
        val body = type.format(mapOf("name" to "디퍼", "week" to 2))
        val payload = mapOf("type" to type.name, "sessionId" to session.value, "memberId" to member.value)
        given(expo.sendBatch(devices, type.title, body, payload, ExpoPriority.NORMAL, retryTransport = false)).willReturn(
            listOf(
                ExpoPushTicket("ok"),
                ExpoPushTicket("error", details = mapOf("error" to "MessageRateExceeded")),
                ExpoPushTicket("error", details = mapOf("error" to "DeviceNotRegistered")),
            ),
        )

        listener.onSubmitted(AbsenceReasonSubmittedEvent(cohort, session, member, "디퍼", 2))

        verify(expo).sendBatch(devices, type.title, body, payload, ExpoPriority.NORMAL, retryTransport = false)
        verify(expo).sendBatch(listOf("limited"), type.title, body, payload, ExpoPriority.NORMAL, retryTransport = false)
        verifyNoMoreInteractions(expo)
    }

    @Test
    fun `Expo가 요청 본문을 받은 뒤 응답 없이 연결을 끊으면 사유서 푸시를 재전송하지 않는다`() {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.use { it.readAllBytes() }
            requests.incrementAndGet()
            exchange.close()
        }
        server.start()
        try {
            val client = ExpoPushClient("http://127.0.0.1:${server.address.port}/", null)
            val subject = AbsenceReasonNotificationListener(staff, AbsenceReasonPushService(tokens, client))
            given(tokens.findTokensForRecipients(listOf(member))).willReturn(listOf("device"))

            subject.onReviewed(AbsenceReasonReviewedEvent(session, member, 2, true))

            assertThat(requests.get()).isEqualTo(1)
        } finally {
            server.stop(0)
        }
    }
}
