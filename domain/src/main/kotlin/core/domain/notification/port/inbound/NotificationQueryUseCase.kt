package core.domain.notification.port.inbound

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken

interface NotificationQueryUseCase {
    fun getPushTokensByMemberId(memberId: MemberId): List<NotificationToken>
}
