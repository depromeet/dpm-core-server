package core.domain.notification.port.outbound

import core.domain.announcement.vo.AssignmentId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessage

interface SentAnnouncementNotificationPersistencePort {
    fun findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        assignmentId: AssignmentId,
        notificationType: NotificationMessage,
    ): List<SentAnnouncementNotification>
}
