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
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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
        val storedToken = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(storedToken))
        val service = createService(provider, port)

        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${provider.generateAccessToken(memberId.toString(), KAKAO_LOGIN)}")
                setCookies(Cookie("refreshToken", storedToken))
            }

        val result = service.reissue(request, MockHttpServletResponse())

        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
    }

    /** 낡은 쿠키가 남아 있어도 헤더의 살아 있는 토큰을 찾아내야 한다. */
    @Test
    fun `쿠키가 폐기된 값이어도 헤더의 유효한 리프레시 토큰으로 재발급된다`() {
        val provider = createProvider()
        val liveToken = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val staleToken = provider.generateRefreshToken("999", KAKAO_LOGIN)
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
        val storedToken = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
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
    fun `재발급된 토큰은 이전 리프레시 토큰의 로그인 계정을 이어받는다`() {
        val provider = createProvider()
        val storedToken = provider.generateRefreshToken(memberId.toString(), APPLE_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(storedToken))
        val service = createService(provider, port)

        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", storedToken)) }

        val result = service.reissue(request, MockHttpServletResponse())

        assertThat(provider.getSessionIdentity(result.accessToken))
            .isEqualTo(APPLE_LOGIN.copy(sessionTokenHash = TokenHasher.sha256Hex(result.refreshToken)))
        assertThat(provider.getLoginIdentity(result.accessToken)).isEqualTo(APPLE_LOGIN)
        assertThat(provider.getLoginIdentity(result.refreshToken)).isEqualTo(APPLE_LOGIN)
    }

    /** 로그인 수단을 판별할 수 없는 이전 세션은 재로그인한다. */
    @Test
    fun `로그인 계정이 없는 이전 토큰은 재발급을 거절한다`() {
        val provider = createProvider()
        val storedToken = provider.generateRefreshToken(memberId.toString(), null)
        val port = FakeRefreshTokenPersistencePort(stored(storedToken))
        val service = createService(provider, port)

        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", storedToken)) }

        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
    }

    @Test
    fun `후보가 모두 저장소에 없으면 TOKEN_NOT_FOUND 를 던진다`() {
        val provider = createProvider()
        val service = createService(provider, FakeRefreshTokenPersistencePort())

        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${provider.generateAccessToken(memberId.toString(), KAKAO_LOGIN)}")
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

    @Test
    fun `삭제 회원은 기존 refresh를 회전하거나 지우지 않고 재발급을 거절한다`() {
        val provider = createProvider()
        val plainToken = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val original = stored(plainToken)
        val port = FakeRefreshTokenPersistencePort(original)
        val service = createService(provider, port, loginAvailable = false)
        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", plainToken)) }
        val response = MockHttpServletResponse()
        assertThatThrownBy { service.reissue(request, response) }.isInstanceOf(TokenInvalidException::class.java)
        assertThat(port.findAllByMemberId(memberId.value)).containsExactly(original)
        assertThat(original.isRotated()).isFalse()
        assertThat(response.getHeaders("Set-Cookie")).isEmpty()
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = LoginMethod::class, names = ["KAKAO", "APPLE"])
    fun `transferred session rotates to retained member for either provider`(method: LoginMethod) {
        val provider = createProvider()
        val identity = if (method == LoginMethod.KAKAO) KAKAO_LOGIN else APPLE_LOGIN
        val old = provider.generateRefreshToken("77", identity)
        val port = FakeRefreshTokenPersistencePort(stored(old))
        val result =
            createService(provider, port, sourceId = 77).reissue(
                MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $old") },
                MockHttpServletResponse(),
            )
        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
        assertThat(provider.getMemberId(result.refreshToken)).isEqualTo(memberId.value)
        assertThat(provider.getLoginIdentity(result.accessToken)).isEqualTo(identity)
        assertThat(port.findByTokenHash(TokenHasher.sha256Hex(old))!!.isRotated()).isTrue()
    }

    @Test
    fun `rotating transferred sessions does not evict retained devices above login cap`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken("77", KAKAO_LOGIN)
        val rows = (1..11).map { stored(provider.generateRefreshToken(memberId.toString(), APPLE_LOGIN)) }
        val port = FakeRefreshTokenPersistencePort(stored(old), *rows.toTypedArray())
        createService(provider, port, sourceId = 77).reissue(
            MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) },
            MockHttpServletResponse(),
        )
        rows.forEach { assertThat(port.findByTokenHash(it.tokenHash)).isNotNull() }
    }

    @Test
    fun `merge changing the discovered path retries after releasing the old transaction`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken("77", KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(old))
        val resolver = mock(core.application.security.oauth.token.MergedMemberResolver::class.java)
        val before = core.application.security.oauth.token.MergedMemberResolver.ResolvedMember(77, listOf(77))
        val after = core.application.security.oauth.token.MergedMemberResolver.ResolvedMember(memberId.value, listOf(77, memberId.value))
        `when`(resolver.resolvePath(old)).thenReturn(before, after, after, after)
        val result =
            createService(provider, port, sourceId = 77, resolver = resolver).reissue(
                MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) },
                MockHttpServletResponse(),
            )
        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
        org.mockito.Mockito.verify(resolver, org.mockito.Mockito.times(4)).resolvePath(old)
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = [77, 335])
    fun `one hundred grace retries return one successor and preserve unrelated devices`(sourceId: Long) {
        val provider = createProvider()
        val old = provider.generateRefreshToken(sourceId.toString(), KAKAO_LOGIN)
        val unrelated = (1..11).map { stored(provider.generateRefreshToken(memberId.toString(), APPLE_LOGIN)) }
        val port = FakeRefreshTokenPersistencePort(stored(old), *unrelated.toTypedArray())
        val service = createService(provider, port, sourceId = sourceId)
        val results =
            (1..100).map {
                service.reissue(MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) }, MockHttpServletResponse())
            }
        assertThat(results.map { it.refreshToken }.distinct()).hasSize(1)
        assertThat(port.findAllByMemberId(memberId.value).filterNot { it.isRotated() }).hasSize(12)
        unrelated.forEach { assertThat(port.findByTokenHash(it.tokenHash)).isNotNull() }
        val successor = port.findByTokenHash(TokenHasher.sha256Hex(results.first().refreshToken))!!
        assertThat(
            provider.generateRefreshToken(
                successor.issuedMemberId.toString(),
                KAKAO_LOGIN,
                successor.tokenJti!!,
                successor.issuedAt,
                successor.expiresAt,
            ),
        ).isEqualTo(results.first().refreshToken)
    }

    @Test
    fun `deleted successor is not recreated by replaying its predecessor`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(old))
        val service = createService(provider, port)
        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) }
        val first = service.reissue(request, MockHttpServletResponse())
        port.deleteByTokenHash(TokenHasher.sha256Hex(first.refreshToken))
        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
        assertThat(port.findAllByMemberId(memberId.value).filterNot { it.isRotated() }).isEmpty()
    }

    @Test
    fun `successor reconstruction refuses a different login identity`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(old))
        val service = createService(provider, port)
        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) }
        val first = service.reissue(request, MockHttpServletResponse())
        val successor = port.findByTokenHash(TokenHasher.sha256Hex(first.refreshToken))!!
        val wrongPlain =
            provider.generateRefreshToken(
                memberId.toString(),
                APPLE_LOGIN,
                successor.tokenJti!!,
                successor.issuedAt,
                successor.expiresAt,
            )
        port.save(
            RefreshToken(
                memberId = memberId,
                tokenHash = TokenHasher.sha256Hex(wrongPlain),
                issuedAt = successor.issuedAt,
                expiresAt = successor.expiresAt,
                tokenJti = successor.tokenJti,
                issuedMemberId = successor.issuedMemberId,
            ),
        )
        port.findByTokenHash(TokenHasher.sha256Hex(old))!!.successorTokenHash = TokenHasher.sha256Hex(wrongPlain)
        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
    }

    @Test
    fun `legacy rotated predecessor acquires only one successor during its grace window`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val predecessor = stored(old).apply { markRotated(Instant.now()) }
        val port = FakeRefreshTokenPersistencePort(predecessor)
        val service = createService(provider, port)
        val results =
            (1..10).map {
                service.reissue(MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) }, MockHttpServletResponse())
            }
        assertThat(results.map { it.refreshToken }.distinct()).hasSize(1)
        assertThat(port.findAllByMemberId(memberId.value).filterNot { it.isRotated() }).hasSize(1)
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = [true, false])
    fun `expired or stale rotated successor is never revived`(expired: Boolean) {
        val provider = createProvider()
        val old = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(old))
        val service = createService(provider, port)
        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) }
        val first = service.reissue(request, MockHttpServletResponse())
        val successor = port.findByTokenHash(TokenHasher.sha256Hex(first.refreshToken))!!
        port.save(
            RefreshToken(
                memberId = memberId,
                tokenHash = successor.tokenHash,
                issuedAt = successor.issuedAt,
                expiresAt = if (expired) Instant.now().minusSeconds(1) else successor.expiresAt,
                tokenJti = successor.tokenJti,
                issuedMemberId = successor.issuedMemberId,
                rotatedAt = if (expired) null else Instant.now().minusSeconds(61),
            ),
        )
        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
        assertThat(port.findAllByMemberId(memberId.value)).hasSize(2)
    }

    @Test
    fun `successor created before merge keeps its signed subject while access uses retained member`() {
        val provider = createProvider()
        val old = provider.generateRefreshToken("77", KAKAO_LOGIN)
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val jti = java.util.UUID.randomUUID().toString()
        val successorPlain = provider.generateRefreshToken("77", KAKAO_LOGIN, jti, now, now.plusSeconds(3600))
        val successor =
            RefreshToken(
                memberId = memberId,
                tokenHash = TokenHasher.sha256Hex(successorPlain),
                issuedAt = now,
                expiresAt = now.plusSeconds(3600),
                tokenJti = jti,
                issuedMemberId = 77,
            )
        val predecessor =
            stored(old).apply {
                markRotated(now)
                successorTokenHash = successor.tokenHash
            }
        val port = FakeRefreshTokenPersistencePort(predecessor, successor)
        val result =
            createService(provider, port, sourceId = 77).reissue(
                MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", old)) },
                MockHttpServletResponse(),
            )
        assertThat(result.refreshToken).isEqualTo(successorPlain)
        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(memberId.value)
        assertThat(port.findAllByMemberId(memberId.value)).hasSize(2)
    }

    @Test
    fun `delayed ancestor retry returns active descendant which still refreshes after ancestor grace`() {
        val provider = createProvider()
        val t0 = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(t0))
        val service = createService(provider, port)

        fun refresh(token: String) =
            service.reissue(
                MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", token)) },
                MockHttpServletResponse(),
            )
        val t1 = refresh(t0).refreshToken
        val t2 = refresh(t1).refreshToken
        val delayed = refresh(t0)
        assertThat(delayed.refreshToken).isEqualTo(t2)
        assertThat(port.findAllByMemberId(memberId.value).filterNot { it.isRotated() }).hasSize(1)
        listOf(t0, t1).forEach { port.findByTokenHash(TokenHasher.sha256Hex(it))!!.rotatedAt = Instant.now().minusSeconds(61) }
        assertThatThrownBy { refresh(t0) }.isInstanceOf(TokenInvalidException::class.java)
        val t3 = refresh(delayed.refreshToken)
        assertThat(provider.getMemberId(t3.accessToken)).isEqualTo(memberId.value)
        assertThat(t3.refreshToken).isNotEqualTo(t2)
    }

    @Test
    fun `cyclic successor chain fails closed without creating a token`() {
        val provider = createProvider()
        val t0 = provider.generateRefreshToken(memberId.toString(), KAKAO_LOGIN)
        val port = FakeRefreshTokenPersistencePort(stored(t0))
        val service = createService(provider, port)
        val request = MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", t0)) }
        val t1 = service.reissue(request, MockHttpServletResponse()).refreshToken
        port.findByTokenHash(TokenHasher.sha256Hex(t1))!!.apply {
            markRotated(Instant.now())
            successorTokenHash = TokenHasher.sha256Hex(t0)
        }
        assertThatThrownBy { service.reissue(request, MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
        assertThat(port.findAllByMemberId(memberId.value)).hasSize(2)
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
        loginAvailable: Boolean = true,
        sourceId: Long = memberId.value,
        resolver: core.application.security.oauth.token.MergedMemberResolver? = null,
    ): RefreshTokenService {
        val securityProperties = testSecurityProperties()
        return RefreshTokenService(
            refreshTokenPersistencePort = port,
            refreshTokenIssueService = RefreshTokenIssueService(provider, TEST_TOKEN_PROPERTIES, port),
            tokenResolver = JwtTokenResolver(),
            tokenInjector = JwtTokenInjector(TEST_TOKEN_PROPERTIES, securityProperties),
            tokenProvider = provider,
            deviceIdResolver = DeviceIdResolver(securityProperties),
            identities =
                mock(core.application.security.oauth.token.LoginIdentityValidator::class.java).also {
                    `when`(it.isValid(memberId.value, KAKAO_LOGIN)).thenReturn(true)
                    `when`(it.isValid(memberId.value, APPLE_LOGIN)).thenReturn(true)
                },
            mergedMembers =
                resolver ?: mock(core.application.security.oauth.token.MergedMemberResolver::class.java).also {
                    `when`(it.resolvePath(org.mockito.ArgumentMatchers.anyString())).thenReturn(core.application.security.oauth.token.MergedMemberResolver.ResolvedMember(memberId.value, listOf(sourceId, memberId.value).distinct()))
                },
            transactions =
                mock(org.springframework.transaction.PlatformTransactionManager::class.java).also {
                    `when`(it.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(org.springframework.transaction.support.SimpleTransactionStatus())
                },
            members =
                mock(MemberPersistencePort::class.java).also {
                    `when`(it.lockApprovalTargets(listOf(sourceId, memberId.value).distinct().sorted())).thenReturn(
                        listOf(sourceId, memberId.value).distinct().sorted().map { id ->
                            MemberApprovalTarget(id, if (id == memberId.value) MemberStatus.ACTIVE else MemberStatus.WITHDRAWN, emptySet(), id != memberId.value || !loginAvailable)
                        },
                    )
                },
        )
    }

    @Test
    fun `해제된 세션의 회전 직전 토큰으로 로그아웃하면 후속 토큰도 폐기한다`() {
        val now = Instant.now()
        val current =
            RefreshToken(
                memberId = memberId,
                tokenHash = "current",
                issuedAt = now,
                expiresAt = now.plusSeconds(60),
                rotatedAt = now,
                successorTokenHash = "next",
                continuationMemberId = memberId.value,
            )
        val next =
            RefreshToken(
                memberId = memberId,
                tokenHash = "next",
                issuedAt = now,
                expiresAt = now.plusSeconds(60),
                continuationMemberId = memberId.value,
            )
        val other = stored("other")
        val store = FakeRefreshTokenPersistencePort(current, next, other)
        RefreshTokenInvalidateService(store).destroyByTokenHash(current.tokenHash)
        assertThat(store.findAllByMemberId(memberId.value)).containsExactly(other)
    }

    private fun createProvider(): JwtTokenProvider = JwtTokenProvider(TEST_TOKEN_PROPERTIES, StubRoleQueryUseCase())

    private fun testSecurityProperties(): SecurityProperties =
        SecurityProperties(
            logoutUrl = "/logout",
            cookie = SecurityProperties.Cookie(domain = "depromeet.com", httpOnly = true, secure = true),
        )

    companion object {
        private val KAKAO_LOGIN = LoginIdentity(LoginMethod.KAKAO, 7L)
        private val APPLE_LOGIN = LoginIdentity(LoginMethod.APPLE, 8L)

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

    override fun lockByTokenHash(tokenHash: String): RefreshToken? = findByTokenHash(tokenHash)

    override fun findAllByMemberId(memberId: Long): List<RefreshToken> = store.filter { it.memberId.value == memberId }

    override fun markRotated(
        tokenHash: String,
        rotatedAt: Instant,
    ) {
        store.firstOrNull { it.tokenHash == tokenHash }?.markRotated(rotatedAt)
    }

    override fun linkSuccessor(
        tokenHash: String,
        successorTokenHash: String,
    ) {
        findByTokenHash(tokenHash)!!.successorTokenHash = successorTokenHash
    }

    override fun grantContinuation(
        tokenHash: String,
        memberId: Long,
    ) {
        findByTokenHash(tokenHash)!!.continuationMemberId = memberId
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
