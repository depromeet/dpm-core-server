package core.application.notification.application.service

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.SentSessionNotificationQueryUseCase
import core.domain.notification.port.outbound.SentSessionNotificationPersistencePort
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service

@Service
class SentSessionNotificationQueryService(
    val sentSessionNotificationPersistencePort: SentSessionNotificationPersistencePort,
) : SentSessionNotificationQueryUseCase {
    override fun findSentSessionNotificationBySessionIdAndNotificationType(
        sessionId: SessionId,
        notificationType: NotificationMessageType,
    ): SentSessionNotification? =
        sentSessionNotificationPersistencePort.findBySessionIdAndNotificationType(
            sessionId = sessionId,
            notificationType = notificationType,
        )
}
