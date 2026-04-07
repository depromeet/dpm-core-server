package core.domain.notification.port.outbound

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.session.vo.SessionId

interface SentSessionNotificationPersistencePort {
    fun findBySessionIdAndNotificationType(
        sessionId: SessionId,
        notificationType: NotificationMessageType,
    ): SentSessionNotification?

    fun findBySessionIdsAndNotificationType(
        sessionIds: List<SessionId>,
        notificationType: NotificationMessageType,
    ): List<SentSessionNotification>

    fun save(sentSessionNotification: SentSessionNotification): SentSessionNotification
}
