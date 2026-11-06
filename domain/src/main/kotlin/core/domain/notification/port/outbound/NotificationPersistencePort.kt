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

    /** 한 기기가 수신 대상 외의 계정에도 등록된 경우 개인 알림을 보내지 않는다. */
    fun findTokensForRecipients(memberIds: List<MemberId>): List<String>

    fun findByMemberIdIn(memberIds: List<MemberId>): List<NotificationToken>
}
