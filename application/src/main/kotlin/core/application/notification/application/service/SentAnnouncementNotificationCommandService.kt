package core.application.notification.application.service

import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.port.inbound.SentAnnouncementNotificationCommandUseCase
import core.domain.notification.port.outbound.SentAnnouncementNotificationPersistencePort
import org.springframework.stereotype.Service

@Service
class SentAnnouncementNotificationCommandService(
    val sentAnnouncementNotificationPersistencePort: SentAnnouncementNotificationPersistencePort,
) : SentAnnouncementNotificationCommandUseCase {
    override fun save(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification =
        sentAnnouncementNotificationPersistencePort.save(sentAnnouncementNotification)

    override fun updateSentAt(
        sentAnnouncementNotification: SentAnnouncementNotification,
    ): SentAnnouncementNotification =
        sentAnnouncementNotificationPersistencePort.updateSentAt(sentAnnouncementNotification)
}
