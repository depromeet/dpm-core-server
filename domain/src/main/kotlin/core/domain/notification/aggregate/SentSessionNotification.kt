package core.domain.notification.aggregate

import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.vo.SentSessionNotificationId
import core.domain.session.vo.SessionId
import java.time.Instant

class SentSessionNotification(
    val sentSessionNotificationId: SentSessionNotificationId,
    val sessionId: SessionId,
    val notificationMessageType: NotificationMessageType,
    val sentAt: Instant? = null,
)
