package core.application.notification.application.service

import core.domain.notification.enums.NotificationMessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotificationPushDataTest {
    @Test
    fun `messageType 이름과 id 를 data 에 합친다`() {
        val data =
            NotificationCommandService.mergePushData(
                NotificationMessageType.SESSION_FEEDBACK_OPENED,
                mapOf("sessionId" to 12L),
            )

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "type" to "SESSION_FEEDBACK_OPENED",
                "sessionId" to 12L,
            ),
        )
    }

    @Test
    fun `호출부 type 이 있어도 messageType 이름이 우선한다`() {
        val data =
            NotificationCommandService.mergePushData(
                NotificationMessageType.ABSENCE_REASON_SUBMITTED,
                mapOf("type" to "ABSENCE_REASON", "sessionId" to 1L),
            )

        assertThat(data["type"]).isEqualTo("ABSENCE_REASON_SUBMITTED")
        assertThat(data["sessionId"]).isEqualTo(1L)
    }

    @Test
    fun `추가 data 가 없어도 type 만 넣는다`() {
        val data =
            NotificationCommandService.mergePushData(
                NotificationMessageType.ATTENDANCE_STARTED,
                null,
            )

        assertThat(data).isEqualTo(mapOf("type" to "ATTENDANCE_STARTED"))
    }
}
