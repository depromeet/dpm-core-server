package core.domain.notification.aggregate

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.vo.SentAnnouncementNotificationId
import java.time.Instant

class SentAnnouncementNotification(
    val sentAnnouncementNotificationId: SentAnnouncementNotificationId,
    val announcementId: AnnouncementId,
    val notificationMessageType: NotificationMessageType,
    val sentAt: Instant? = null,
)
