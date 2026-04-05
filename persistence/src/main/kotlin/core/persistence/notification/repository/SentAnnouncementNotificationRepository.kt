package core.persistence.notification.repository

import core.domain.announcement.vo.AssignmentId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessage
import core.domain.notification.port.outbound.SentAnnouncementNotificationPersistencePort
import org.springframework.stereotype.Repository

@Repository
class SentAnnouncementNotificationRepository() : SentAnnouncementNotificationPersistencePort {
    override fun findSentAnnouncementNotificationByAssignmentIdAndNotificationType(
        assignmentId: AssignmentId,
        notificationType: NotificationMessage,
    ): List<SentAnnouncementNotification> {
        TODO("Not yet implemented")
    }
//    TODO : 1분 스케줄러 짜서 알림 발송 로직 구현
}
