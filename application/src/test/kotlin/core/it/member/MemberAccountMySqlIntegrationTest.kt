package core.it.member

import core.application.common.configuration.JooqDslConfig
import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberOAuthConflictException
import core.application.member.application.service.MemberAccountLinkRequestService
import core.application.member.application.service.MemberAccountService
import core.application.member.application.service.MemberIdentityLockService
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.refreshToken.application.service.RefreshTokenService
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.service.OAuthAuthorizationUrlService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.application.security.oauth.token.LoginIdentityValidator
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberAccountPersistencePort
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.OAuthLinkRequest
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.security.oauth.dto.KakaoAuthAttributes
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.persistence.member.repository.MemberAccountRepository
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.oauth.MemberOAuthRepository
import core.persistence.membercredential.repository.MemberCredentialRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = [MemberAccountMySqlIntegrationTest.Config::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemberAccountMySqlIntegrationTest {
    @Autowired lateinit var service: MemberAccountService

    @Autowired lateinit var identities: LoginIdentityValidator

    @Autowired lateinit var requests: MemberAccountLinkRequestService

    @Autowired lateinit var accounts: MemberAccountPersistencePort

    @Autowired lateinit var oauth: MemberOAuthPersistencePort

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var tx: PlatformTransactionManager

    @Autowired lateinit var refresh: RefreshTokenService

    @Autowired lateinit var tokenStore: RefreshTokenPersistencePort

    @Autowired lateinit var tokenProvider: JwtTokenProvider

    @Autowired lateinit var issuer: RefreshTokenIssueService
    private val kakao = LoginIdentity(LoginMethod.KAKAO, 10)
    private val apple = LoginIdentity(LoginMethod.APPLE, 11)

    @BeforeEach
    fun fixture() {
        reset(tokenStore, tokenProvider, issuer)
        jdbc.execute("alter table member_oauth modify external_id varchar(255) character set utf8mb4 collate utf8mb4_bin not null")
        listOf("member_oauth_link_requests", "member_credentials", "member_oauth", "member_cohorts", "members").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into members(member_id,name,signup_email,status,created_at) values(1,'홍길동','same@example.com','PENDING',now(6)),(2,'김철수','same@example.com','PENDING',now(6))")
        jdbc.update("insert into member_oauth(member_oauth_id,member_id,provider,external_id,email) values(10,1,'KAKAO','kakao','same@example.com'),(11,1,'APPLE','apple','same@example.com'),(12,2,'KAKAO','other','same@example.com')")
    }

    @Test
    fun `연결 조회는 실제 자격증명만 이메일 로그인으로 포함한다`() {
        assertThat(service.get(1).accounts.map { it.loginMethod }).containsExactly(LoginMethod.KAKAO, LoginMethod.APPLE)
        jdbc.update("insert into member_credentials(member_id,email,password,created_at,updated_at) values(1,'email@example.com','hash',now(6),now(6))")
        val result = service.get(1).accounts
        assertThat(result.map { it.loginMethod }).containsExactly(LoginMethod.KAKAO, LoginMethod.APPLE, LoginMethod.EMAIL)
        assertThat(result.last().unlinkAllowed).isFalse()
    }

    @Test
    fun `해제된 로그인 식별자는 재연결 후에도 사용할 수 없다`() {
        service.unlink(1, kakao, 11)
        assertThat(identities.isValid(1, apple)).isFalse()
        assertThat(identities.isValid(1, kakao)).isTrue()
        val attributes = KakaoAuthAttributes("apple", "same@example.com", "홍길동", OAuthProvider.APPLE)
        service.link(1, kakao, attributes)
        assertThat(accounts.findOAuths(1)).hasSize(2)
        assertThat(identities.isValid(1, apple)).isFalse()
        service.link(1, kakao, attributes)
        assertThat(accounts.findOAuths(1)).hasSize(2)
    }

    @Test
    fun `다른 회원의 연결은 가져오지 않으며 이메일이 같아도 통합하지 않는다`() {
        assertThatThrownBy { service.link(1, kakao, KakaoAuthAttributes("other", "same@example.com", "홍길동", OAuthProvider.KAKAO)) }.isInstanceOf(MemberOAuthConflictException::class.java)
        assertThat(oauth.findById(MemberOAuthId(12))!!.memberId).isEqualTo(MemberId(2))
        service.unlink(1, kakao, 12)
        assertThat(accounts.findOAuths(2)).hasSize(1)
    }

    @Test
    fun `동시 해제는 마지막 수단을 남기고 해제된 세션의 재요청을 거절한다`() {
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val results =
                listOf(10L to kakao, 11L to apple).map { (id, identity) ->
                    pool.submit(
                        Callable {
                            start.await()
                            runCatching { service.unlink(1, identity, id) }
                        },
                    )
                }
            start.countDown()
            assertThat(results.map { it.get(10, TimeUnit.SECONDS) }.count { it.isSuccess }).isEqualTo(1)
            val remaining = accounts.findOAuths(1).single()
            assertThatThrownBy { service.unlink(1, LoginIdentity(LoginMethod.from(remaining.provider), remaining.id!!.value), remaining.id!!.value) }.isInstanceOf(MemberAccountException::class.java)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `삭제 회원과 로그인 식별자가 없는 세션은 연결을 변경하지 못한다`() {
        assertThatThrownBy { service.unlink(1, null, 11) }.isInstanceOf(MemberAccountException::class.java)
        jdbc.update("update members set deleted_at=now(6), status='WITHDRAWN' where member_id=1")
        assertThatThrownBy { service.unlink(1, kakao, 11) }.isInstanceOf(core.application.member.application.exception.MemberDeletedException::class.java)
        assertThat(accounts.findOAuths(1)).hasSize(2)
    }

    @Test
    fun `동일 연결의 동시 요청은 한 행만 저장한다`() {
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val futures =
                (1..2).map {
                    pool.submit(
                        Callable {
                            start.await()
                            service.link(1, kakao, KakaoAuthAttributes("new", "same@example.com", "홍길동", OAuthProvider.KAKAO))
                        },
                    )
                }
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            assertThat(accounts.findOAuths(1).count { it.externalId == "new" }).isEqualTo(1)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `다른 회원의 동일 계정 동시 연결은 하나만 성공하고 나머지는 충돌이다`() {
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val futures =
                listOf(1L to kakao, 2L to LoginIdentity(LoginMethod.KAKAO, 12)).map { (member, identity) ->
                    pool.submit(
                        Callable {
                            start.await()
                            runCatching { service.link(member, identity, KakaoAuthAttributes("contended", "same@example.com", "홍길동", OAuthProvider.KAKAO)) }
                        },
                    )
                }
            start.countDown()
            val results = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(MemberOAuthConflictException::class.java)
            assertThat(jdbc.queryForObject("select count(*) from member_oauth where external_id='contended'", Int::class.java)).isEqualTo(1)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `연결 요청은 소유자 제공자 만료를 검증하고 동시에 한 번만 소비된다`() {
        val state = "a".repeat(43)
        val hash = core.application.refreshToken.application.support.TokenHasher.sha256Hex(state)
        val expiry = Instant.now().plusSeconds(300)
        accounts.saveLinkRequest(OAuthLinkRequest(hash, 1, OAuthProvider.APPLE, "https://example.com/callback", "b".repeat(43), expiry))
        assertThatThrownBy { requests.consume(2, OAuthProvider.APPLE, state) }.isInstanceOf(MemberAccountException::class.java)
        assertThatThrownBy { requests.consume(1, OAuthProvider.KAKAO, state) }.isInstanceOf(MemberAccountException::class.java)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map { pool.submit(Callable { runCatching { requests.consume(1, OAuthProvider.APPLE, state) } }) }.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.single { it.isSuccess }.getOrThrow().expiresAt.epochSecond).isEqualTo(expiry.epochSecond)
        } finally {
            pool.shutdownNow()
        }
        accounts.saveLinkRequest(OAuthLinkRequest(hash, 1, OAuthProvider.APPLE, "https://example.com/callback", "b".repeat(43), Instant.now().minusSeconds(1)))
        assertThatThrownBy { requests.consume(1, OAuthProvider.APPLE, state) }.isInstanceOf(MemberAccountException::class.java)
        requests.removeExpired()
        assertThat(jdbc.queryForObject("select count(*) from member_oauth_link_requests", Int::class.java)).isZero()
    }

    @Test
    fun `네이티브 연결 요청은 웹 경로와 다른 회원이 소비하지 못하고 한 번만 성공한다`() {
        val state = requests.startNativeKakao(1, apple)
        assertThat(state).hasSize(43)
        assertThatThrownBy { requests.consume(1, OAuthProvider.KAKAO, state) }.isInstanceOf(MemberAccountException::class.java)
        assertThatThrownBy { requests.consumeNativeKakao(2, state) }.isInstanceOf(MemberAccountException::class.java)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = (1..2).map { pool.submit(Callable { runCatching { requests.consumeNativeKakao(1, state) } }) }.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(MemberAccountException::class.java)
        } finally {
            pool.shutdownNow()
        }
        assertThat(jdbc.queryForObject("select count(*) from member_oauth_link_requests", Int::class.java)).isZero()
    }

    @Test
    fun `웹 연결 요청을 네이티브 경로에서 소비할 수 없다`() {
        val state = "w".repeat(43)
        accounts.saveLinkRequest(OAuthLinkRequest(core.application.refreshToken.application.support.TokenHasher.sha256Hex(state), 1, OAuthProvider.KAKAO, "https://example.com/callback", "verifier", Instant.now().plusSeconds(300)))
        assertThatThrownBy { requests.consumeNativeKakao(1, state) }.isInstanceOf(MemberAccountException::class.java)
        assertThat(requests.consume(1, OAuthProvider.KAKAO, state).memberId).isEqualTo(1)
    }

    @Test
    fun `네이티브 연결 시작은 유효한 로그인 식별자가 필요하고 만료 요청은 소비되지 않는다`() {
        assertThatThrownBy { requests.startNativeKakao(1, null) }.isInstanceOf(MemberAccountException::class.java)
        val state = requests.startNativeKakao(1, apple)
        jdbc.update("update member_oauth_link_requests set expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)))
        assertThatThrownBy { requests.consumeNativeKakao(1, state) }.isInstanceOf(MemberAccountException::class.java)
    }

    @Test
    fun `네이티브 완료는 기존 회원에 연결하며 신규 회원을 만들지 않는다`() {
        val state = requests.startNativeKakao(1, apple)
        nativeLinkService().complete(1, apple, state, "provider-token")
        assertThat(oauth.findByProviderAndExternalId(OAuthProvider.KAKAO, "456")!!.memberId).isEqualTo(MemberId(1))
        assertThat(jdbc.queryForObject("select count(*) from members", Int::class.java)).isEqualTo(2)
        assertThatThrownBy { nativeLinkService().complete(1, apple, state, "provider-token") }.isInstanceOf(MemberAccountException::class.java)
    }

    @Test
    fun `네이티브 인증 중 현재 로그인 수단을 해제하면 완료를 거절한다`() {
        val state = requests.startNativeKakao(1, apple)
        service.unlink(1, kakao, 11)
        assertThatThrownBy { nativeLinkService().complete(1, apple, state, "provider-token") }.isInstanceOf(MemberAccountException::class.java)
        assertThat(oauth.findByProviderAndExternalId(OAuthProvider.KAKAO, "456")).isNull()
        assertThat(jdbc.queryForObject("select count(*) from member_oauth_link_requests", Int::class.java)).isZero()
    }

    private fun nativeLinkService(): core.application.member.application.service.KakaoNativeAccountLinkService {
        val provider = mock(core.application.security.oauth.kakao.KakaoUserInfoClient::class.java)
        `when`(provider.getAccessTokenInfo("provider-token")).thenReturn(core.application.security.oauth.kakao.KakaoAccessTokenInfo(456, 123, 300))
        `when`(provider.getUserAttributesByAccessToken("provider-token")).thenReturn(mapOf("id" to 456L))
        return core.application.member.application.service.KakaoNativeAccountLinkService(requests, service, provider, 123)
    }

    @Test
    fun `해제가 회원 잠금을 선점하면 앞서 토큰을 읽은 재발급도 해제 결과를 확인한다`() {
        val observed = CountDownLatch(1)
        val unlinked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val hash = core.application.refreshToken.application.support.TokenHasher.sha256Hex("refresh")
        `when`(tokenProvider.validateToken("refresh")).thenReturn(true)
        `when`(tokenProvider.getLoginIdentity("refresh")).thenReturn(kakao)
        `when`(tokenStore.findByTokenHash(hash)).thenAnswer {
            // This consistent read establishes the snapshot before waiting on the member lock.
            assertThat(jdbc.queryForObject("select count(*) from member_oauth where member_oauth_id=10", Int::class.java)).isEqualTo(1)
            observed.countDown()
            RefreshToken(1, MemberId(1), hash, null, Instant.now(), Instant.now().plusSeconds(3600))
        }
        `when`(tokenStore.lockByTokenHash(hash)).thenReturn(
            RefreshToken(1, MemberId(1), hash, null, Instant.now(), Instant.now().plusSeconds(3600)),
        )
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        TransactionTemplate(tx).executeWithoutResult {
                            service.unlink(1, apple, 10)
                            unlinked.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(unlinked.await(10, TimeUnit.SECONDS))
            val second =
                pool.submit(
                    Callable {
                        runCatching { refresh.reissue(MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", "refresh")) }, MockHttpServletResponse()) }
                    },
                )
            check(observed.await(10, TimeUnit.SECONDS))
            release.countDown()
            first.get(10, TimeUnit.SECONDS)
            assertThat(second.get(10, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(TokenInvalidException::class.java)
            verifyNoInteractions(issuer)
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["core.entity"])
    @EnableJpaRepositories(basePackages = ["core.persistence"])
    @Import(JooqDslConfig::class, MemberRepository::class, MemberMergeRepository::class, MemberOAuthRepository::class, MemberCredentialRepository::class, MemberAccountRepository::class, MemberIdentityLockService::class, LoginIdentityValidator::class, MemberAccountService::class, MemberAccountLinkRequestService::class)
    class Config {
        @Bean fun tokenStore(): RefreshTokenPersistencePort = mock(RefreshTokenPersistencePort::class.java)

        @Bean fun tokenProvider(): JwtTokenProvider = mock(JwtTokenProvider::class.java)

        @Bean fun issuer(): RefreshTokenIssueService = mock(RefreshTokenIssueService::class.java)

        @Bean
        fun refresh(
            store: RefreshTokenPersistencePort,
            provider: JwtTokenProvider,
            issuer: RefreshTokenIssueService,
            members: MemberPersistencePort,
            identities: LoginIdentityValidator,
        ): RefreshTokenService = RefreshTokenService(store, issuer, JwtTokenResolver(), mock(JwtTokenInjector::class.java), provider, mock(DeviceIdResolver::class.java), members, identities)

        @Bean fun redirects(): OAuthRedirectUriValidator = mock(OAuthRedirectUriValidator::class.java)

        @Bean fun urls(): OAuthAuthorizationUrlService = mock(OAuthAuthorizationUrlService::class.java)
    }

    companion object {
        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
