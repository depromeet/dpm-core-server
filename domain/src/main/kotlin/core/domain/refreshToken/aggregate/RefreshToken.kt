package core.domain.refreshToken.aggregate

import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * 리프레시 토큰 한 개. 한 기기의 세션 체인 한 마디에 해당한다.
 *
 * DB 에는 [tokenHash] 만 조회 키로 저장한다. [plainToken] 은 발급 직후에만 값이 있고
 * DB 에서 읽어온 객체에서는 항상 null 이다.
 */
class RefreshToken(
    val tokenId: Long? = null,
    val memberId: MemberId,
    val tokenHash: String,
    val deviceId: String? = null,
    val issuedAt: Instant,
    val expiresAt: Instant,
    var rotatedAt: Instant? = null,
    val plainToken: String? = null,
) {
    fun requirePlainToken(): String = plainToken ?: error("평문 토큰은 발급 직후에만 존재합니다. tokenId=$tokenId")

    fun isExpired(now: Instant): Boolean = expiresAt.isBefore(now)

    fun isRotated(): Boolean = rotatedAt != null

    /** 회전된 토큰이 유예 시간 안에 다시 들어왔는지. 동시 재발급을 허용하기 위한 구간이다. */
    fun isWithinGrace(
        now: Instant,
        graceSeconds: Long,
    ): Boolean {
        val rotated = rotatedAt ?: return false
        return !rotated.plusSeconds(graceSeconds).isBefore(now)
    }

    fun markRotated(at: Instant) {
        this.rotatedAt = at
    }

    /**
     * 동일성은 [tokenHash] 기준이다.
     *
     * 이전 구현은 memberId 만 비교했다. 회원당 다중 행이 되면 서로 다른 기기의 토큰이
     * 같다고 판정되어 distinct / Set / remove 가 조용히 오동작한다.
     * tokenId 가 아니라 tokenHash 를 쓰는 이유는 저장 전(tokenId == null)에도 식별이 되기 때문이다.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshToken) return false
        return tokenHash == other.tokenHash
    }

    override fun hashCode(): Int = tokenHash.hashCode()

    companion object {
        fun issue(
            memberId: MemberId,
            plainToken: String,
            tokenHash: String,
            deviceId: String?,
            issuedAt: Instant,
            expiresAt: Instant,
        ): RefreshToken =
            RefreshToken(
                tokenId = null,
                memberId = memberId,
                tokenHash = tokenHash,
                deviceId = deviceId,
                issuedAt = issuedAt,
                expiresAt = expiresAt,
                rotatedAt = null,
                plainToken = plainToken,
            )
    }
}
