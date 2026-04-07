package core.domain.notification.port.inbound

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.session.vo.SessionId

interface SentSessionNotificationQueryUseCase {
    fun findSentSessionNotificationBySessionIdAndNotificationType(
        sessionId: SessionId,
        notificationType: NotificationMessageType,
    ): SentSessionNotification?
}
