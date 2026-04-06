package core.persistence.notification.repository

import core.domain.announcement.vo.AssignmentId
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
        assignmentId: AssignmentId,
        notificationType: NotificationMessageType,
    ): List<SentAnnouncementNotification> =
        sentAnnouncementNotificationJpaRepository
            .findByAnnouncementIdAndNotificationMessageType(assignmentId.value, notificationType)
            .map { it.toDomain() }

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
