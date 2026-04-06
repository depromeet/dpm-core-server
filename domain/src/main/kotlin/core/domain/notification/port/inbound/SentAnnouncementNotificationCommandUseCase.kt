package core.domain.notification.port.inbound

import core.domain.notification.aggregate.SentAnnouncementNotification

interface SentAnnouncementNotificationCommandUseCase {
    fun save(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification

    fun updateSentAt(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification
}
