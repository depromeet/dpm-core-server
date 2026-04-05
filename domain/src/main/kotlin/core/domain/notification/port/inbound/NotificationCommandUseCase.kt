package core.domain.notification.port.inbound

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken

interface NotificationCommandUseCase {
    fun registerPushToken(
        memberId: MemberId,
        token: String,
    ): NotificationToken

    fun deletePushToken(
        memberId: MemberId,
        token: String,
    )

    fun deleteAllPushTokens(memberId: MemberId)

    fun sendCustomPushNotificationToMembers(
        memberIds: List<MemberId>,
        title: String,
        body: String,
        data: Map<String, Any>? = null,
    )
}
