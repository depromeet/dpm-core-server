package core.application.notification.presentation.request

import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType

data class MessageTypeNotificationRequest(
    val targetMemberId: MemberId,
    val notificationMessageType: NotificationMessageType,
    val title: String,
)
