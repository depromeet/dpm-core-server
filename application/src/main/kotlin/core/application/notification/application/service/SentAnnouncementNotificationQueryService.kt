package core.application.notification.application.service

import core.domain.announcement.vo.AssignmentId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.SentAnnouncementNotificationQueryUseCase
import core.domain.notification.port.outbound.SentAnnouncementNotificationPersistencePort
import org.springframework.stereotype.Service

@Service
class SentAnnouncementNotificationQueryService(
    val sentAnnouncementNotificationPersistencePort: SentAnnouncementNotificationPersistencePort,
) : SentAnnouncementNotificationQueryUseCase {
    override fun getSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        assignmentId: AssignmentId,
        notificationType: NotificationMessageType,
    ): List<SentAnnouncementNotification> =
        sentAnnouncementNotificationPersistencePort.findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
            assignmentId = assignmentId,
            notificationType,
        )
}
