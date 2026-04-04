package core.domain.notification.event

import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessage

data class NotificationByMemberEvent(
    val notificationMessage: NotificationMessage,
    val memberId: MemberId,
) {
    companion object {
        fun of(
            notificationMessage: NotificationMessage,
            memberId: MemberId,
        ) = NotificationByMemberEvent(
            notificationMessage = notificationMessage,
            memberId = memberId,
        )
    }
}
