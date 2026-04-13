package core.application.notification.application.service

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.notification.port.outbound.SentSessionNotificationPersistencePort
import org.springframework.stereotype.Service

@Service
class SentSessionNotificationCommandService(
    val sentSessionNotificationPersistencePort: SentSessionNotificationPersistencePort,
) : SentSessionNotificationCommandUseCase {
    override fun save(sentSessionNotification: SentSessionNotification): SentSessionNotification =
        sentSessionNotificationPersistencePort.save(sentSessionNotification)

    override fun updateSentAt(sentSessionNotification: SentSessionNotification): SentSessionNotification =
        sentSessionNotificationPersistencePort.save(sentSessionNotification)
}
