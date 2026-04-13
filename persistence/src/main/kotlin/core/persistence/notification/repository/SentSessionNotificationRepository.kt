package core.persistence.notification.repository

import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.outbound.SentSessionNotificationPersistencePort
import core.domain.session.vo.SessionId
import core.entity.notification.SentSessionNotificationEntity
import org.springframework.stereotype.Repository

@Repository
class SentSessionNotificationRepository(
    val sentSessionNotificationJpaRepository: SentSessionNotificationJpaRepository,
) : SentSessionNotificationPersistencePort {
    override fun findBySessionIdAndNotificationType(
        sessionId: SessionId,
        notificationType: NotificationMessageType,
    ): SentSessionNotification? =
        sentSessionNotificationJpaRepository
            .findBySessionIdAndNotificationMessageType(sessionId.value, notificationType.name)
            ?.toDomain()

    override fun findBySessionIdsAndNotificationType(
        sessionIds: List<SessionId>,
        notificationType: NotificationMessageType,
    ): List<SentSessionNotification> =
        sentSessionNotificationJpaRepository
            .findBySessionIdInAndNotificationMessageType(
                sessionIds.map { it.value },
                notificationType.name,
            ).map { it.toDomain() }

    override fun save(sentSessionNotification: SentSessionNotification): SentSessionNotification =
        sentSessionNotificationJpaRepository.save(SentSessionNotificationEntity.from(sentSessionNotification))
            .toDomain()
}
