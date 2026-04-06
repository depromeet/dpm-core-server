package core.persistence.notification.repository

import core.domain.notification.enums.NotificationMessageType
import core.entity.notification.SentAnnouncementNotificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SentAnnouncementNotificationJpaRepository : JpaRepository<SentAnnouncementNotificationEntity, Long> {
    fun findByAnnouncementIdAndNotificationMessageType(
        announcementId: Long,
        notificationMessageType: NotificationMessageType,
    ): List<SentAnnouncementNotificationEntity>
}
