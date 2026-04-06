package core.domain.notification.event

import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType

data class NotificationByMemberEvent(
    val notificationMessageType: NotificationMessageType,
    val memberId: MemberId,
) {
    companion object {
        fun of(
            notificationMessageType: NotificationMessageType,
            memberId: MemberId,
        ) = NotificationByMemberEvent(
            notificationMessageType = notificationMessageType,
            memberId = memberId,
        )
    }
}
