package core.domain.notification.aggregate

import core.domain.member.vo.MemberId
import core.domain.notification.vo.NotificationTokenId
import java.time.Instant

/**
 * PushToken을 표현하는 도메인 모델이며, 애그리거트 루트입니다.
 *
 * 이 도메인은 회원의 푸시 알림 토큰 정보를 나타냅니다.
 * 한 회원은 여러 기기를 사용할 수 있으므로 1:N 관계입니다.
 *
 * Expo Push Token 형식: ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]
 */
class NotificationToken(
    val id: NotificationTokenId? = null,
    val memberId: MemberId,
    val token: String,
    createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    var createdAt: Instant? = createdAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    fun updateTimestamp() {
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationToken) return false

        return id == other.id &&
            memberId == other.memberId &&
            token == other.token
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + token.hashCode()
        return result
    }

    companion object {
        fun create(
            memberId: MemberId,
            token: String,
        ): NotificationToken {
            val now = Instant.now()
            return NotificationToken(
                memberId = memberId,
                token = token,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
