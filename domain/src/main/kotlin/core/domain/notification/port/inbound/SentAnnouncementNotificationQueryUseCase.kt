package core.domain.notification.port.inbound

import core.domain.announcement.vo.AnnouncementId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType

interface SentAnnouncementNotificationQueryUseCase {
    fun findAll(): List<SentAnnouncementNotification>

    /**
     * 특정 과제에 대해 특정 유형의 알림이 이미 발송되었는지 확인합니다.
     * 과제가 없어도 Exception을 터트릴 게 아니기 때문에 find로 했습니다.
     */
    fun findSentAnnouncementNotificationByAnnouncementIdAndNotificationType(
        announcementId: AnnouncementId,
        notificationType: NotificationMessageType,
    ): SentAnnouncementNotification?
}
