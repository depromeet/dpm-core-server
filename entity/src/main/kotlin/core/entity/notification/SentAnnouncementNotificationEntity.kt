package core.entity.notification

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.vo.SentAnnouncementNotificationId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "sent_announcement_notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_sent_announcement_notification",
            columnNames = ["announcement_id", "notification_message"],
        ),
    ],
)
class SentAnnouncementNotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sent_announcement_notification_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "announcement_id", nullable = false)
    val announcementId: Long,
    @Column(name = "notification_message", nullable = false, length = 30)
    val notificationMessageType: String,
    @Column(name = "sent_at")
    val sentAt: Instant? = null,
) {
    companion object {
        fun from(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotificationEntity =
            SentAnnouncementNotificationEntity(
                id = sentAnnouncementNotification.sentAnnouncementNotificationId.value,
                announcementId = sentAnnouncementNotification.announcementId.value,
                notificationMessageType = sentAnnouncementNotification.notificationMessageType.name,
                sentAt = sentAnnouncementNotification.sentAt,
            )
    }

    fun toDomain(): SentAnnouncementNotification =
        SentAnnouncementNotification(
            sentAnnouncementNotificationId = SentAnnouncementNotificationId(id),
            announcementId = AnnouncementId(announcementId),
            notificationMessageType = NotificationMessageType.valueOf(notificationMessageType),
            sentAt = sentAt,
        )
}
