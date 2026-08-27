package core.domain.refreshToken.port.outbound

import core.domain.refreshToken.aggregate.RefreshToken
import java.time.Instant

interface RefreshTokenPersistencePort {
    fun save(refreshToken: RefreshToken): RefreshToken

    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun findAllByMemberId(memberId: Long): List<RefreshToken>

    fun markRotated(
        tokenHash: String,
        rotatedAt: Instant,
    )

    fun deleteByTokenHash(tokenHash: String)

    fun deleteByMemberId(memberId: Long)

    fun deleteByMemberIdAndDeviceId(
        memberId: Long,
        deviceId: String,
    )

    fun deleteExpired(now: Instant): Int

    /** 회전이 끝나고 유예 시간까지 지난 행을 정리한다. 활성 토큰 상한 계산을 오염시키지 않기 위함이다. */
    fun deleteRotatedBefore(threshold: Instant): Int
}
