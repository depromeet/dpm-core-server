package core.persistence.notification.repository

import core.entity.notification.SentAnnouncementNotificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SentAnnouncementNotificationJpaRepository : JpaRepository<SentAnnouncementNotificationEntity, Long> {
    fun findByAnnouncementIdAndNotificationMessageType(
        announcementId: Long,
        notificationMessageType: String,
    ): SentAnnouncementNotificationEntity?
}
