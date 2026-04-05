package core.domain.notification.aggregate

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.enums.NotificationMessage
import core.domain.notification.vo.SentAnnouncementNotificationId
import java.time.LocalDateTime

class SentAnnouncementNotification(
    val sentAnnouncementNotificationId: SentAnnouncementNotificationId,
    val announcementId: AnnouncementId,
    val notificationMessage: NotificationMessage,
    val sentAt: LocalDateTime,
)
