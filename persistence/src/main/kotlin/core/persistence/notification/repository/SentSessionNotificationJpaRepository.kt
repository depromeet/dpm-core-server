package core.persistence.notification.repository

import core.entity.notification.SentSessionNotificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SentSessionNotificationJpaRepository : JpaRepository<SentSessionNotificationEntity, Long> {
    fun findBySessionIdAndNotificationMessageType(
        sessionId: Long,
        notificationMessageType: String,
    ): SentSessionNotificationEntity?

    fun findBySessionIdInAndNotificationMessageType(
        sessionIds: List<Long>,
        notificationMessageType: String,
    ): List<SentSessionNotificationEntity>
}
