package core.domain.notification.port.inbound

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken

interface NotificationQueryUseCase {
    fun getPushTokensByMemberId(memberId: MemberId): List<NotificationToken>

    /**
     * 특정 회원이 등록한 푸시 토큰 중에서 주어진 토큰과 일치하는 토큰을 찾습니다.
     * 일치하는 토큰이 없으면 null을 반환합니다.
     * 의도적으로 nullable하게 반환합니다.
     */
    fun findPushTokensByMemberIdAndToken(
        memberId: MemberId,
        token: String,
    ): NotificationToken?
}
