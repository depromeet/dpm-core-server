package core.application.refreshToken.application.service

import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.exception.TokenNotFoundException
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant

class RefreshTokenServiceTest {
    private val memberId = MemberId(335L)

    /**
     * 이번 장애의 형태. 클라이언트는 일반 API 인증용으로 Authorization 에 액세스 토큰을 싣는데,
     * 이 브랜치의 리졸버는 Bearer 를 쿠키보다 먼저 둔다. 액세스와 리프레시는 서명 키도 클레임도
     * 같아 validateToken 만으로는 구분되지 않으므로, 저장소 존재 여부로 판정하지 않으면
     * 액세스 토큰이 채택되어 R404 로 떨어진다.
     */
    @Test
    fun `Authorization 에 액세스 토큰이 실려 있어도 쿠키의 리프레시 토큰으로 재발급된다`() {
        val provider = createProvider()
        val storedToken = provider.generateRefreshToken(memberId.toString())
        val port = FakeRefreshTokenPersistencePort(stored(storedToken))
        val service = createService(provider, port)

        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${provider.generateAccessToken(memberId.toString())}")
                setCookies(Cookie("refreshToken", storedToken))
            }

        val result = service.reissue(request, MockHttpServletResponse())

        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
    }

    /** 낡은 쿠키가 남아 있어도 헤더의 살아 있는 토큰을 찾아내야 한다. */
    @Test
    fun `쿠키가 폐기된 값이어도 헤더의 유효한 리프레시 토큰으로 재발급된다`() {
        val provider = createProvider()
        val liveToken = provider.generateRefreshToken(memberId.toString())
        val staleToken = provider.generateRefreshToken("999")
        val port = FakeRefreshTokenPersistencePort(stored(liveToken))
        val service = createService(provider, port)

        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer $liveToken")
                setCookies(Cookie("refreshToken", staleToken))
            }

        val result = service.reissue(request, MockHttpServletResponse())

        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
    }

    @Test
    fun `재발급에 성공하면 이전 토큰이 회전 표시되고 새 토큰이 쿠키로 내려간다`() {
        val provider = createProvider()
        val storedToken = provider.generateRefreshToken(memberId.toString())
        val port = FakeRefreshTokenPersistencePort(stored(storedToken))
        val service = createService(provider, port)

        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", storedToken)) }
        val response = MockHttpServletResponse()

        val result = service.reissue(request, response)

        assertThat(port.findByTokenHash(TokenHasher.sha256Hex(storedToken))!!.isRotated()).isTrue()
        assertThat(response.getHeaders("Set-Cookie"))
            .anyMatch { it.startsWith("refreshToken=${result.refreshToken};") }
            .anyMatch { it.startsWith("accessToken=${result.accessToken};") }
    }

    @Test
    fun `후보가 모두 저장소에 없으면 TOKEN_NOT_FOUND 를 던진다`() {
        val provider = createProvider()
        val service = createService(provider, FakeRefreshTokenPersistencePort())

        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${provider.generateAccessToken(memberId.toString())}")
            }

        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }
            .isInstanceOf(TokenNotFoundException::class.java)
    }

    @Test
    fun `유효한 JWT 후보가 하나도 없으면 TOKEN_INVALID 를 던진다`() {
        val provider = createProvider()
        val service = createService(provider, FakeRefreshTokenPersistencePort())

        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", "not-a-jwt")) }

        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }
            .isInstanceOf(TokenInvalidException::class.java)
    }

    private fun stored(plainToken: String): RefreshToken {
        val now = Instant.now()
        return RefreshToken(
            tokenId = 1L,
            memberId = memberId,
            tokenHash = TokenHasher.sha256Hex(plainToken),
            deviceId = "device-1",
            issuedAt = now,
            expiresAt = now.plusSeconds(2592000L),
        )
    }

    private fun createService(
        provider: JwtTokenProvider,
        port: RefreshTokenPersistencePort,
    ): RefreshTokenService {
        val securityProperties = testSecurityProperties()
        return RefreshTokenService(
            refreshTokenPersistencePort = port,
            refreshTokenIssueService = RefreshTokenIssueService(provider, TEST_TOKEN_PROPERTIES, port),
            tokenResolver = JwtTokenResolver(),
            tokenInjector = JwtTokenInjector(TEST_TOKEN_PROPERTIES, securityProperties),
            tokenProvider = provider,
            deviceIdResolver = DeviceIdResolver(securityProperties),
        )
    }

    private fun createProvider(): JwtTokenProvider = JwtTokenProvider(TEST_TOKEN_PROPERTIES, StubRoleQueryUseCase())

    private fun testSecurityProperties(): SecurityProperties =
        SecurityProperties(
            logoutUrl = "/logout",
            cookie = SecurityProperties.Cookie(domain = "depromeet.com", httpOnly = true, secure = true),
        )

    companion object {
        /** HMAC 키 길이 요건(256bit 이상)을 채우기 위한 테스트 전용 BASE64 시크릿. */
        private val TEST_TOKEN_PROPERTIES =
            TokenProperties(
                secretKey = "dGVzdC1zZWNyZXQta2V5LWZvci1kcG0tY29yZS1yZWZyZXNoLXRva2VuLXNlcnZpY2UtdGVzdA==",
                expirationTime = TokenProperties.ExpirationTime(accessToken = 7200L, refreshToken = 2592000L),
            )
    }
}

private class FakeRefreshTokenPersistencePort(
    vararg initial: RefreshToken,
) : RefreshTokenPersistencePort {
    private val store = initial.toMutableList()

    override fun save(refreshToken: RefreshToken): RefreshToken {
        store.removeAll { it.tokenHash == refreshToken.tokenHash }
        store.add(refreshToken)
        return refreshToken
    }

    override fun findByTokenHash(tokenHash: String): RefreshToken? = store.firstOrNull { it.tokenHash == tokenHash }

    override fun findAllByMemberId(memberId: Long): List<RefreshToken> = store.filter { it.memberId.value == memberId }

    override fun markRotated(
        tokenHash: String,
        rotatedAt: Instant,
    ) {
        store.firstOrNull { it.tokenHash == tokenHash }?.markRotated(rotatedAt)
    }

    override fun deleteByTokenHash(tokenHash: String) {
        store.removeAll { it.tokenHash == tokenHash }
    }

    override fun deleteByMemberId(memberId: Long) {
        store.removeAll { it.memberId.value == memberId }
    }

    override fun deleteByMemberIdAndDeviceId(
        memberId: Long,
        deviceId: String,
    ) {
        store.removeAll { it.memberId.value == memberId && it.deviceId == deviceId }
    }

    override fun deleteExpired(now: Instant): Int = 0

    override fun deleteRotatedBefore(threshold: Instant): Int = 0
}

private class StubRoleQueryUseCase : RoleQueryUseCase {
    override fun getAllRoles(): List<Role> = emptyList()

    override fun getRolesByCohort(cohort: String): List<Role> = emptyList()

    override fun getRoleNamesByMemberId(memberId: MemberId): List<String> = emptyList()

    override fun getRoleNamesByMemberIds(memberIds: List<MemberId>): Map<MemberId, List<String>> = emptyMap()

    override fun getRolesByExternalId(externalId: String): List<String> = emptyList()

    override fun getPermissionsByMemberId(memberId: MemberId): List<String> = emptyList()

    override fun findIdByName(roleName: String): Long = 0L
}
