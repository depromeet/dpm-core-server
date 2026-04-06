package core.domain.notification.event

import core.domain.member.aggregate.InviteTagSpec
import core.domain.notification.enums.NotificationMessageType

data class InviteTagNotificationEvent(
    val notificationMessageType: NotificationMessageType,
    val inviteTags: List<InviteTagSpec>,
) {
    companion object {
        fun of(
            notificationMessageType: NotificationMessageType,
            inviteTags: List<InviteTagSpec>,
        ): InviteTagNotificationEvent =
            InviteTagNotificationEvent(
                notificationMessageType = notificationMessageType,
                inviteTags = inviteTags,
            )
    }
}
