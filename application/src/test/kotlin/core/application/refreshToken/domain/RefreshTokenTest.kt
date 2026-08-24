package core.application.refreshToken.domain

import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class RefreshTokenTest {
    private val memberId = MemberId(335L)
    private val now: Instant = Instant.parse("2026-08-25T00:00:00Z")

    private fun token(
        hash: String,
        deviceId: String? = null,
        expiresAt: Instant = now.plusSeconds(2_592_000),
        rotatedAt: Instant? = null,
    ) = RefreshToken(
        tokenId = null,
        memberId = memberId,
        tokenHash = hash,
        deviceId = deviceId,
        issuedAt = now,
        expiresAt = expiresAt,
        rotatedAt = rotatedAt,
        plainToken = "plain-$hash",
    )

    @Test
    fun `memberId가 같아도 tokenHash가 다르면 서로 다른 토큰이다`() {
        val deviceA = token("hash-a", deviceId = "device-a")
        val deviceB = token("hash-b", deviceId = "device-b")

        // 이전 구현은 memberId만 비교해서 두 기기의 토큰을 같다고 판정했다.
        assertThat(deviceA).isNotEqualTo(deviceB)
        assertThat(setOf(deviceA, deviceB)).hasSize(2)
        assertThat(listOf(deviceA, deviceB).distinct()).hasSize(2)
    }

    @Test
    fun `tokenHash가 같으면 같은 토큰이다`() {
        assertThat(token("hash-a")).isEqualTo(token("hash-a"))
    }

    @Test
    fun `만료 여부는 expiresAt 기준으로 판정한다`() {
        assertThat(token("h", expiresAt = now.minusSeconds(1)).isExpired(now)).isTrue()
        assertThat(token("h", expiresAt = now.plusSeconds(1)).isExpired(now)).isFalse()
    }

    @Test
    fun `회전되지 않은 토큰은 유예 구간에 있지 않다`() {
        val notRotated = token("h")

        assertThat(notRotated.isRotated()).isFalse()
        assertThat(notRotated.isWithinGrace(now, 60L)).isFalse()
    }

    @Test
    fun `회전 후 60초 이내는 유예, 초과하면 재사용이다`() {
        val withinGrace = token("h", rotatedAt = now.minusSeconds(59))
        val beyondGrace = token("h", rotatedAt = now.minusSeconds(61))

        assertThat(withinGrace.isWithinGrace(now, 60L)).isTrue()
        assertThat(beyondGrace.isWithinGrace(now, 60L)).isFalse()
    }

    @Test
    fun `평문 토큰이 없으면 requirePlainToken은 실패한다`() {
        val fromDatabase =
            RefreshToken(
                tokenId = 1L,
                memberId = memberId,
                tokenHash = "hash",
                issuedAt = now,
                expiresAt = now.plusSeconds(60),
            )

        assertThat(fromDatabase.plainToken).isNull()
        assertThat(runCatching { fromDatabase.requirePlainToken() }.isFailure).isTrue()
    }
}
