package core.domain.deviceToken.aggregate

import core.domain.deviceToken.vo.DeviceTokenId
import core.domain.member.vo.MemberId
import java.time.Instant

class DeviceToken(
    val id: DeviceTokenId? = null,
    val memberId: MemberId,
    var token: String,
    val createdAt: Instant? = null,
    var updatedAt: Instant? = null,
) {
    companion object {
        fun create(
            memberId: MemberId,
            token: String,
        ): DeviceToken {
            val now = Instant.now()
            return DeviceToken(
                memberId = memberId,
                token = token,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun updateToken(newToken: String) {
        this.token = newToken
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceToken) return false
        return memberId == other.memberId && token == other.token
    }

    override fun hashCode(): Int = 31 * memberId.hashCode() + token.hashCode()
}
