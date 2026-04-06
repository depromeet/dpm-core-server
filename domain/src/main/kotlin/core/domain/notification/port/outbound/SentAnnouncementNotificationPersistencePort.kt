package core.domain.notification.port.outbound

import core.domain.announcement.vo.AssignmentId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType

interface SentAnnouncementNotificationPersistencePort {
    fun findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        assignmentId: AssignmentId,
        notificationType: NotificationMessageType,
    ): List<SentAnnouncementNotification>
}
