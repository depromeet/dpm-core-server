package core.domain.notification.port.inbound

import core.domain.announcement.vo.AssignmentId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessage

interface SentAnnouncementNotificationQueryUseCase {
    fun getSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        assignmentId: AssignmentId,
        notificationType: NotificationMessage,
    ): List<SentAnnouncementNotification>
}
