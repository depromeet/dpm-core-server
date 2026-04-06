package core.persistence.notification.repository

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.outbound.SentAnnouncementNotificationPersistencePort
import core.entity.notification.SentAnnouncementNotificationEntity
import org.springframework.stereotype.Repository

@Repository
class SentAnnouncementNotificationRepository(
    val sentAnnouncementNotificationJpaRepository: SentAnnouncementNotificationJpaRepository,
) : SentAnnouncementNotificationPersistencePort {
    override fun findAll(): List<SentAnnouncementNotification> =
        sentAnnouncementNotificationJpaRepository.findAll().map { it.toDomain() }

    override fun findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        announcementId: AnnouncementId,
        notificationType: NotificationMessageType,
    ): SentAnnouncementNotification? =
        sentAnnouncementNotificationJpaRepository
            .findByAnnouncementIdAndNotificationMessageType(announcementId.value, notificationType.name)
            ?.toDomain()

    override fun save(sentAnnouncementNotification: SentAnnouncementNotification): SentAnnouncementNotification {
        val entity = SentAnnouncementNotificationEntity.from(sentAnnouncementNotification)
        val savedEntity = sentAnnouncementNotificationJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun updateSentAt(
        sentAnnouncementNotification: SentAnnouncementNotification,
    ): SentAnnouncementNotification {
        val entity = SentAnnouncementNotificationEntity.from(sentAnnouncementNotification)
        val updatedEntity = sentAnnouncementNotificationJpaRepository.save(entity)
        return updatedEntity.toDomain()
    }
}
