package core.domain.notification.port.outbound

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType

interface SentAnnouncementNotificationPersistencePort {
    fun findAll(): List<SentAnnouncementNotification>

    fun findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        announcementId: AnnouncementId,
        notificationType: NotificationMessageType,
    ): SentAnnouncementNotification?

    fun save(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification

    fun updateSentAt(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification
}
