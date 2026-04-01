package core.domain.notification.port.outbound

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.vo.NotificationTokenId

interface NotificationPersistencePort {
    fun save(notificationToken: NotificationToken): NotificationToken

    fun findByMemberId(memberId: MemberId): List<NotificationToken>

    fun findByMemberIdAndToken(
        memberId: MemberId,
        token: String,
    ): NotificationToken?

    fun deleteByToken(token: String)

    fun deleteByMemberId(memberId: MemberId)

    fun findById(id: NotificationTokenId): NotificationToken?

    fun findByMemberIdIn(memberIds: List<MemberId>): List<NotificationToken>
}
