package core.it.member

import core.application.authorization.application.service.RoleQueryService
import core.application.common.configuration.JooqDslConfig
import core.application.member.application.exception.InvalidEmailPasswordException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.MemberCommandService
import core.application.member.application.service.MemberIdentityLockService
import core.application.member.application.service.MemberLoginService
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.MemberProfileService
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.presentation.request.AppleMemberProfileUpdateRequest
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.security.oauth.dto.KakaoAuthAttributes
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.MemberProfileRepository
import core.persistence.member.repository.oauth.MemberOAuthRepository
import core.persistence.membercredential.repository.MemberCredentialRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = [MemberIdentityMySqlIntegrationTest.Config::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemberIdentityMySqlIntegrationTest {
    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var oauths: MemberOAuthPersistencePort

    @Autowired lateinit var credentials: MemberCredentialPersistencePort

    @Autowired lateinit var profiles: core.domain.member.port.outbound.MemberProfilePersistencePort

    @Autowired lateinit var locks: MemberIdentityLockService

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var tx: PlatformTransactionManager

    @BeforeEach
    fun fixture() {
        listOf("member_credentials", "member_oauth", "member_cohorts", "members").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at) values (1, '홍길동', 'social@example.com', 'SERVER', 'PENDING', now(6))")
    }

    @Test
    fun `이메일만 같은 소셜 계정은 비밀번호와 토큰을 만들지 않는다`() {
        val tokens = stub<JwtTokenProvider>()
        val issuer = stub<RefreshTokenIssueService>()
        val service = EmailPasswordAuthService(credentials, members, locks, stub(), stub(), stub(), tokens, issuer, BCryptPasswordEncoder())
        listOf("PENDING", "REJECTED", "ACTIVE", "INACTIVE").forEach { status ->
            jdbc.update("update members set status = ? where member_id = 1", status)
            assertThatThrownBy { rc().execute { service.login("social@example.com", "arbitrary-password") } }
                .isInstanceOf(InvalidEmailPasswordException::class.java)
        }
        assertThat(jdbc.queryForObject("select count(*) from member_credentials", Int::class.java)).isZero()
        verifyNoInteractions(tokens, issuer)
    }

    @Test
    fun `새 이메일 가입과 저장된 비밀번호 검증 로그인은 유지된다`() {
        val roles = stub<RoleQueryService>()
        // Test doubles supply only token values; member and credential persistence remain real.
        val tokenAnswer =
            org.mockito.stubbing.Answer<Any> { invocation ->
                if (invocation.method.name.startsWith("issueForLogin")) {
                    RefreshToken(memberId = MemberId(invocation.getArgument<Number>(0).toLong()), tokenHash = "hash", plainToken = "refresh", issuedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(60))
                } else {
                    org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val realIssuer = mock(RefreshTokenIssueService::class.java, tokenAnswer)
        val realTokens =
            mock(
                JwtTokenProvider::class.java,
                org.mockito.stubbing.Answer { invocation ->
                    if (invocation.method.name == "generateAccessTokenWithPermissions") "access" else org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation)
                },
            )
        val service = EmailPasswordAuthService(credentials, members, locks, roles, stub(), stub(), realTokens, realIssuer, BCryptPasswordEncoder())
        assertThat(rc().execute { service.login("new@example.com", "password") }!!.accessToken).isEqualTo("access")
        assertThat(rc().execute { service.login("new@example.com", "password") }!!.accessToken).isEqualTo("access")
        assertThatThrownBy { rc().execute { service.login("new@example.com", "wrong") } }.isInstanceOf(InvalidEmailPasswordException::class.java)
        assertThat(jdbc.queryForObject("select count(*) from member_credentials", Int::class.java)).isEqualTo(1)
    }

    @Test
    fun `고아 OAuth 로그인은 잠금 검증 후 기존 회원으로 복구하고 삭제 회원은 복구하지 않는다`() {
        jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider) values (10, 999, 'external', 'KAKAO')")
        val issuer = stub<RefreshTokenIssueService>()
        val identity = LoginIdentity(LoginMethod.KAKAO, 10)
        `when`(issuer.issueForLogin(MemberId(1), null, identity)).thenReturn(RefreshToken(memberId = MemberId(1), tokenHash = "hash", plainToken = "refresh", issuedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(60)))
        val service = MemberLoginService(members, locks, MemberOAuthService(oauths), issuer, stub(), stub())
        val attributes = KakaoAuthAttributes("external", "social@example.com", "홍길동", OAuthProvider.KAKAO)
        rc().execute { service.handleLoginSuccess(attributes, null) }
        assertThat(jdbc.queryForObject("select member_id from member_oauth where member_oauth_id = 10", Long::class.java)).isEqualTo(1)
        jdbc.update("update members set status = 'WITHDRAWN', deleted_at = now(6) where member_id = 1")
        assertThatThrownBy { rc().execute { service.handleLoginSuccess(attributes, null) } }.isInstanceOf(MemberDeletedException::class.java)
        assertThat(jdbc.queryForObject("select count(*) from members", Int::class.java)).isEqualTo(1)
    }

    @Test
    fun `기존 미배정 파트 회원도 로그인한 뒤 이름과 파트를 최초 입력한다`() {
        jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider) values (10, 1, 'external', 'KAKAO')")
        val issuer = stub<RefreshTokenIssueService>()
        val identity = LoginIdentity(LoginMethod.KAKAO, 10)
        val issued = RefreshToken(memberId = MemberId(1), tokenHash = "hash", plainToken = "refresh", issuedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(60))
        `when`(issuer.issueForLogin(MemberId(1), null, identity)).thenReturn(issued)
        val login = MemberLoginService(members, locks, MemberOAuthService(oauths), issuer, stub(), stub())
        val profile = MemberProfileService(profiles)
        val attributes = KakaoAuthAttributes("external", "social@example.com", "홍길동", OAuthProvider.KAKAO)

        listOf("", "UNASSIGNED", "UNKNOWN").forEach { previousPart ->
            jdbc.update("update members set part=?, profile_completed_at=null where member_id=1", previousPart)

            assertThat(rc().execute { login.handleLoginSuccess(attributes, null) }!!.refreshToken).isSameAs(issued)
            assertThat(profile.get(1).profileCompletionRequired).isTrue()
            assertThat(profile.get(1).part).isNull()
            assertThat(jdbc.queryForObject("select part from members where member_id=1", String::class.java)).isEqualTo(previousPart)

            rc().execute { members.save(members.findById(MemberId(1))!!) }
            assertThat(jdbc.queryForObject("select part from members where member_id=1", String::class.java)).isNull()
            rc().execute { profile.complete(1, "김철수", "WEB") }

            assertThat(profile.get(1).name).isEqualTo("김철수")
            assertThat(profile.get(1).part).isEqualTo("WEB")
            assertThat(profile.get(1).profileCompletionRequired).isFalse()
        }
    }

    @Test
    fun `고아 OAuth 복구 대상이 잠금 대기 중 삭제되면 연결을 옮기지 않는다`() {
        jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider) values (10, 999, 'external', 'KAKAO')")
        val issuer = stub<RefreshTokenIssueService>()
        val service = MemberLoginService(members, locks, MemberOAuthService(oauths), issuer, stub(), stub())
        val attributes = KakaoAuthAttributes("external", "social@example.com", "홍길동", OAuthProvider.KAKAO)
        val started = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        try {
            lateinit var future: java.util.concurrent.Future<Result<Any>>
            rc().executeWithoutResult {
                locks.lockMember(MemberId(1))
                jdbc.update("update members set status = 'WITHDRAWN', deleted_at = now(6) where member_id = 1")
                future =
                    pool.submit(
                        Callable {
                            started.countDown()
                            runCatching { rc().execute { service.handleLoginSuccess(attributes, null) } as Any }
                        },
                    )
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
                Thread.sleep(150)
                assertThat(future.isDone).isFalse()
            }
            assertThat(future.get(10, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(MemberDeletedException::class.java)
            assertThat(jdbc.queryForObject("select member_id from member_oauth where member_oauth_id = 10", Long::class.java)).isEqualTo(999)
            verifyNoInteractions(issuer)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `프로필은 반려 커밋 이후 상태를 보존하고 삭제 커밋 이후 저장하지 않는다`() {
        jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider) values (10, 1, 'apple', 'APPLE')")
        val queries = stub<MemberQueryService>()
        `when`(queries.getMemberById(MemberId(1))).thenAnswer { members.findById(MemberId(1))!! }
        val service = MemberCommandService(members, queries, stub(), stub(), stub(), stub(), stub(), MemberOAuthService(oauths), credentials, stub(), stub(), MemberProfileService(profiles))
        val pool = Executors.newSingleThreadExecutor()
        try {
            listOf(false, true).forEach { deleted ->
                val started = CountDownLatch(1)
                lateinit var future: java.util.concurrent.Future<Result<Any>>
                rc().executeWithoutResult {
                    locks.lockMember(MemberId(1))
                    jdbc.update("update members set status = ?, deleted_at = ? where member_id = 1", if (deleted) "WITHDRAWN" else "REJECTED", if (deleted) java.sql.Timestamp.from(Instant.now()) else null)
                    future =
                        pool.submit(
                            Callable {
                                started.countDown()
                                runCatching { rc().execute { service.updateAppleMemberProfile(MemberId(1), AppleMemberProfileUpdateRequest("김철수", "WEB")) } as Any }
                            },
                        )
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
                    Thread.sleep(150)
                    assertThat(future.isDone).isFalse()
                }
                val result = future.get(10, TimeUnit.SECONDS)
                if (deleted) {
                    assertThat(result.exceptionOrNull()).isInstanceOf(MemberDeletedException::class.java)
                } else {
                    assertThat(result.isSuccess).isTrue()
                }
                assertThat(jdbc.queryForObject("select status from members where member_id = 1", String::class.java)).isEqualTo(if (deleted) "WITHDRAWN" else "REJECTED")
            }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun rc() = TransactionTemplate(tx).apply { isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED }

    private inline fun <reified T> stub(): T = mock(T::class.java)

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["core.entity"])
    @EnableJpaRepositories(basePackages = ["core.persistence"])
    @Import(JooqDslConfig::class, MemberRepository::class, MemberProfileRepository::class, MemberMergeRepository::class, MemberOAuthRepository::class, MemberCredentialRepository::class, MemberIdentityLockService::class)
    class Config

    companion object {
        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
