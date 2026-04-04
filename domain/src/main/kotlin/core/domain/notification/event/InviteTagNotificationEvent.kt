package core.domain.notification.event

import core.domain.member.aggregate.InviteTagSpec
import core.domain.notification.enums.NotificationMessage

data class InviteTagNotificationEvent(
    val notificationMessage: NotificationMessage,
    val inviteTags: List<InviteTagSpec>,
) {
    companion object {
        fun of(
            notificationMessage: NotificationMessage,
            inviteTags: List<InviteTagSpec>,
        ): InviteTagNotificationEvent =
            InviteTagNotificationEvent(
                notificationMessage = notificationMessage,
                inviteTags = inviteTags,
            )
    }
}
