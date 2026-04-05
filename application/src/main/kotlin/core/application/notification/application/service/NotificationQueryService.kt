package core.application.notification.application.service

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.port.inbound.NotificationQueryUseCase
import core.domain.notification.port.outbound.NotificationPersistencePort
import org.springframework.stereotype.Service

@Service
class NotificationQueryService(
    val notificationPersistencePort: NotificationPersistencePort,
) : NotificationQueryUseCase {
    override fun getPushTokensByMemberId(memberId: MemberId): List<NotificationToken> =
        notificationPersistencePort.findByMemberId(memberId)
}
