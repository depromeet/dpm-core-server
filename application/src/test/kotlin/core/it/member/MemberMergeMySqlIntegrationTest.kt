package core.it.member

import core.application.common.configuration.JooqDslConfig
import core.application.member.application.exception.InvalidMemberMergeException
import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberOAuthConflictException
import core.application.member.application.service.MemberAccountService
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberIdentityLockService
import core.application.member.application.service.MemberMergeService
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.exception.TokenNotFoundException
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.refreshToken.application.service.RefreshTokenService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.application.security.oauth.token.MergedMemberResolver
import core.application.security.properties.TokenProperties
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.persistence.member.repository.MemberAccountRepository
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import core.persistence.refreshToken.repository.RefreshTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
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
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = [MemberMergeMySqlIntegrationTest.Config::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemberMergeMySqlIntegrationTest {
    @Autowired lateinit var service: MemberMergeService

    @Autowired lateinit var accounts: MemberAccountService

    @Autowired lateinit var locks: MemberIdentityLockService

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var tx: PlatformTransactionManager

    @Autowired lateinit var tokenStore: core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort

    @Autowired lateinit var identities: core.application.security.oauth.token.LoginIdentityValidator

    @Autowired lateinit var mergeStore: core.domain.member.port.outbound.MemberMergePersistencePort

    @MockitoBean lateinit var cohorts: CohortQueryUseCase

    @MockitoBean lateinit var roles: RoleQueryUseCase

    @MockitoBean lateinit var initializer: MemberActivationInitializer

    @BeforeEach
    fun fixture() {
        MemberActivityFixtures.clear(jdbc)
        listOf("attendances", "member_merge_events", "member_oauth_link_requests", "refresh_tokens", "member_credentials", "member_oauth", "member_roles", "member_teams", "member_cohorts", "members", "roles", "teams", "cohorts").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'GUEST')")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (1, 1, 19, 0, 0)")
        (1L..2L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at) values (?, ?, ?, 'SERVER', 'PENDING', now(6))", id, "member-$id", "member-$id@example.test")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
            jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, 2, 19, now(6))", id)
            jdbc.update("insert into member_teams (member_id, team_id) values (?, 1)", id)
            jdbc.update("update members set profile_completed_at = created_at where member_id = ?", id)
            jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider, email) values (?, ?, ?, ?, ?)", id, id, "external-$id", if (id == 1L) "KAKAO" else "APPLE", "oauth-$id@example.test")
        }
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(cohorts.findActiveCohortId()).thenReturn(CohortId(19))
        `when`(roles.findIdByName("DEEPER")).thenReturn(1)
        (1L..2L).forEach { id ->
            jdbc.update("insert into refresh_tokens (member_id, token_hash, device_id, issued_at, expires_at) values (?, ?, 'test-device', now(6), date_add(now(6), interval 1 day))", id, "test-token-$id".padEnd(64, 'x'))
            jdbc.update("insert into member_oauth_link_requests (state_hash, member_id, provider, redirect_uri, code_verifier, expires_at) values (?, ?, 'APPLE', 'https://example.test/callback', 'test-verifier', date_add(now(6), interval 5 minute))", "state-$id".padEnd(64, 'x'), id)
        }
        core.it.member.MemberActivityFixtures.seed(jdbc)
    }

    @Test
    fun `승인 회원을 남기는 병합도 미제출 원본이면 연결을 이전하지 않는다`() {
        jdbc.update("update members set status='ACTIVE' where member_id=1")
        jdbc.update("update members set profile_completed_at=null where member_id=2")
        val tables = listOf("members", "member_oauth", "member_roles", "member_cohorts", "member_merge_events")
        val before = tables.associateWith { jdbc.queryForList("select * from $it") }
        assertThatThrownBy { service.mergeAndApprove(1, 2, 99) }
            .isInstanceOf(core.application.common.exception.BusinessException::class.java)
            .hasMessage("이름과 파트 입력을 완료해주세요")
        before.forEach { (table, rows) -> assertThat(jdbc.queryForList("select * from $table")).isEqualTo(rows) }
    }

    @Test
    fun `원본 연결을 보존하고 OAuth ID를 이전하며 유지 계정을 승인한다`() {
        service.mergeAndApprove(1, 2, 99)
        assertThat(jdbc.queryForMap("select name, part, status from members where member_id = 1"))
            .containsEntry("name", "member-1").containsEntry("part", "SERVER").containsEntry("status", "ACTIVE")
        assertThat(jdbc.queryForObject("select deleted_at is not null from members where member_id = 2", Boolean::class.java)).isTrue()
        assertThat(jdbc.queryForObject("select count(*) from member_oauth where member_id = 1", Int::class.java)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select member_id from member_oauth where member_oauth_id = 2", Long::class.java)).isEqualTo(1)
        listOf("member_cohorts", "member_roles", "member_teams").forEach { table ->
            assertThat(jdbc.queryForObject("select count(*) from $table where member_id = 2", Int::class.java)).isEqualTo(1)
        }
        assertThat(jdbc.queryForObject("select count(*) from member_teams where member_id = 1", Int::class.java)).isZero()
        verify(initializer).initialize(MemberId(1), CohortId(19))
        assertCleanupAndAudit()
        assertThatThrownBy { service.mergeAndApprove(1, 2, 99) }.isInstanceOf(InvalidMemberMergeException::class.java)
    }

    @Test
    fun `승인된 유지 회원의 프로필 기수 팀 역할과 기존 데이터는 바꾸지 않는다`() {
        jdbc.update("update members set status = 'ACTIVE', approval_welcome_seen = true, profile_completed_at = now(6), updated_at = now(6) where member_id = 1")
        jdbc.update("update roles set name = 'CORE' where role_id = 1")
        jdbc.update("update member_roles set role_id = 1 where member_id = 1")
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (18, '18', false, 0, 0)")
        jdbc.update("insert into member_cohorts (member_id, cohort_id) values (1, 18)")
        jdbc.update("insert into attendances (session_id, member_id, status, attended_at, updated_at) values (19, 1, 'PRESENT', now(6), now(6))")
        MemberActivityFixtures.seed(jdbc)
        val before =
            listOf("members", "member_cohorts", "member_roles", "member_teams", "attendances").associateWith {
                jdbc.queryForList("select * from $it where member_id = 1")
            }

        service.mergeAndApprove(1, 2, 99)

        before.forEach { (table, rows) -> assertThat(jdbc.queryForList("select * from $table where member_id = 1")).isEqualTo(rows) }
        assertThat(jdbc.queryForObject("select count(*) from member_oauth where member_id = 1", Int::class.java)).isEqualTo(2)
        verifyNoInteractions(initializer)
        assertCleanupAndAudit()
    }

    @Test
    fun `같은 승인 회원으로 동시 병합 요청은 한 번만 기록한다`() {
        jdbc.update("update members set status = 'ACTIVE' where member_id = 1")
        val ready = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures =
                (1..2).map {
                    pool.submit(
                        Callable {
                            ready.countDown()
                            check(ready.await(10, TimeUnit.SECONDS))
                            runCatching { service.mergeAndApprove(1, 2, 99) }
                        },
                    )
                }
            val results = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(InvalidMemberMergeException::class.java)
            assertCleanupAndAudit()
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `승인 초기화 실패는 OAuth 이전과 원본 삭제까지 취소한다`() {
        doThrow(IllegalStateException("initialization failed")).`when`(initializer).initialize(MemberId(1), CohortId(19))
        assertThatThrownBy { service.mergeAndApprove(1, 2, 99) }.isInstanceOf(IllegalStateException::class.java)
        assertUnchanged()
    }

    @Test
    fun `동일 provider 충돌은 변경 없이 거절한다`() {
        jdbc.update("update member_oauth set provider = 'KAKAO' where member_id = 2")
        assertThatThrownBy { service.mergeAndApprove(1, 2, 99) }.isInstanceOf(MemberOAuthConflictException::class.java)
        assertUnchanged()
    }

    @Test
    fun `어느 계정이든 비밀번호 정보가 있으면 거절한다`() {
        listOf(1L, 2L).forEach { id ->
            jdbc.update("insert into member_credentials (member_id, email, password, created_at, updated_at) values (?, ?, 'test-hash', now(6), now(6))", id, "password-$id@example.test")
            assertThatThrownBy { service.mergeAndApprove(1, 2, 99) }.isInstanceOf(InvalidMemberMergeException::class.java)
            assertUnchanged()
            jdbc.execute("delete from member_credentials")
        }
    }

    @Test
    fun `서로 반대 방향 통합은 하나만 성공한다`() {
        val ready = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures =
                listOf(1L to 2L, 2L to 1L).map { (retained, source) ->
                    pool.submit(
                        Callable {
                            ready.countDown()
                            check(ready.await(10, TimeUnit.SECONDS))
                            runCatching { service.mergeAndApprove(retained, source, 99) }
                        },
                    )
                }
            val results = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(InvalidMemberMergeException::class.java)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `통합 전 OAuth를 읽은 로그인은 통합 커밋을 기다린 뒤 거절된다`() {
        val observed = MemberOAuth(MemberOAuthId(2), "external-2", OAuthProvider.APPLE, MemberId(2), null)
        val started = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        try {
            lateinit var future: java.util.concurrent.Future<Result<Unit>>
            TransactionTemplate(tx).executeWithoutResult {
                members.lockApprovalTargets(listOf(1, 2))
                future =
                    pool.submit(
                        Callable {
                            started.countDown()
                            runCatching { TransactionTemplate(tx).executeWithoutResult { locks.lockOAuth(observed) } }
                        },
                    )
                check(started.await(10, TimeUnit.SECONDS))
                service.mergeAndApprove(1, 2, 99)
            }
            assertThat(future.get(20, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(MemberDeletedException::class.java)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun assertUnchanged() {
        assertThat(jdbc.queryForObject("select count(*) from members where status = 'PENDING' and deleted_at is null", Int::class.java)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select member_id from member_oauth where member_oauth_id = 2", Long::class.java)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select count(*) from member_merge_events", Int::class.java)).isZero()
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens", Int::class.java)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select count(*) from member_oauth_link_requests", Int::class.java)).isEqualTo(2)
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = [1, 2])
    fun `either provider keeps access and refresh after merging into an approved member`(sourceId: Long) {
        val retainedId = if (sourceId == 1L) 2L else 1L
        jdbc.update("update members set status='ACTIVE' where member_id=?", retainedId)
        val properties =
            core.application.security.properties.TokenProperties(
                secretKey = java.util.Base64.getEncoder().encodeToString("merge-session-test-signing-key-with-256-bits".toByteArray()),
                expirationTime = core.application.security.properties.TokenProperties.ExpirationTime(7200, 2592000),
            )
        val provider = core.application.security.oauth.token.JwtTokenProvider(properties, roles)
        val identity =
            core.domain.member.vo.LoginIdentity(
                if (sourceId == 1L) core.domain.member.enums.LoginMethod.KAKAO else core.domain.member.enums.LoginMethod.APPLE,
                sourceId,
            )
        val issuer = core.application.refreshToken.application.service.RefreshTokenIssueService(provider, properties, tokenStore)
        val old = TransactionTemplate(tx).execute { issuer.issueForLogin(MemberId(sourceId), "mobile", identity) }!!
        assertThat(jdbc.queryForObject("select token is null from refresh_tokens where token_hash=?", Boolean::class.java, old.tokenHash)).isTrue()
        val access = provider.generateAccessToken(sourceId.toString(), identity)
        service.mergeAndApprove(retainedId, sourceId, 99)
        val resolver = core.application.security.oauth.token.MergedMemberResolver(provider, members, mergeStore, identities, tokenStore)
        assertThat(resolver.resolve(access)!!.memberId).isEqualTo(retainedId)
        assertThat(tokenStore.findByTokenHash(old.tokenHash)!!.memberId.value).isEqualTo(retainedId)
        val refresh =
            core.application.refreshToken.application.service.RefreshTokenService(
                tokenStore, issuer, core.application.security.oauth.token.JwtTokenResolver(),
                org.mockito.Mockito.mock(core.application.security.oauth.token.JwtTokenInjector::class.java), provider,
                org.mockito.Mockito.mock(core.application.security.oauth.token.DeviceIdResolver::class.java), members, identities, resolver, tx,
            )
        val result =
            refresh.reissue(
                org.springframework.mock.web.MockHttpServletRequest().apply { addHeader("Authorization", "Bearer ${old.requirePlainToken()}") },
                org.springframework.mock.web.MockHttpServletResponse(),
            )
        assertThat(provider.getMemberId(result.accessToken)).isEqualTo(retainedId)
        assertThat(provider.getLoginIdentity(result.refreshToken)).isEqualTo(identity)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val retries =
                (1..100).map {
                    pool.submit(
                        Callable {
                            refresh.reissue(
                                org.springframework.mock.web.MockHttpServletRequest().apply {
                                    addHeader("Authorization", "Bearer ${old.requirePlainToken()}")
                                },
                                org.springframework.mock.web.MockHttpServletResponse(),
                            ).refreshToken
                        },
                    )
                }.map { it.get(30, TimeUnit.SECONDS) }
            assertThat(retries.distinct()).containsExactly(result.refreshToken)
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where rotated_at is null", Int::class.java)).isEqualTo(3)
            (1L..2L).forEach { id ->
                assertThat(tokenStore.findByTokenHash("test-token-$id".padEnd(64, 'x'))).isNotNull()
            }
        } finally {
            pool.shutdownNow()
        }
        TransactionTemplate(tx).executeWithoutResult { tokenStore.deleteByMemberId(retainedId) }
        assertThat(resolver.resolve(old.requirePlainToken())).isNull()
        jdbc.update("delete from member_oauth where member_oauth_id=?", sourceId)
        assertThat(resolver.resolve(access)).isNull()
    }

    @ParameterizedTest
    @ValueSource(longs = [1, 2])
    fun `병합된 회원이 현재 소셜을 해제한 뒤에도 요청한 기기만 세션을 갱신한다`(sourceId: Long) {
        val retainedId = if (sourceId == 1L) 2L else 1L
        val source = LoginIdentity(if (sourceId == 1L) LoginMethod.KAKAO else LoginMethod.APPLE, sourceId)
        val remaining = LoginIdentity(if (retainedId == 1L) LoginMethod.KAKAO else LoginMethod.APPLE, retainedId)
        val properties =
            TokenProperties(
                secretKey = Base64.getEncoder().encodeToString("unlink-session-test-signing-key-with-256-bits".toByteArray()),
                expirationTime = TokenProperties.ExpirationTime(7200, 2592000),
            )
        val provider = JwtTokenProvider(properties, roles)
        val issuer = RefreshTokenIssueService(provider, properties, tokenStore)
        val resolver = MergedMemberResolver(provider, members, mergeStore, identities, tokenStore)
        val refresh =
            RefreshTokenService(
                tokenStore, issuer, JwtTokenResolver(), mock(JwtTokenInjector::class.java), provider,
                mock(DeviceIdResolver::class.java), members, identities, resolver, tx,
            )

        fun login(
            id: Long,
            device: String,
            identity: LoginIdentity,
        ) = TransactionTemplate(tx).execute { issuer.issueForLogin(MemberId(id), device, identity) }!!.requirePlainToken()

        fun rotate(token: String) =
            refresh.reissue(
                MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $token") },
                MockHttpServletResponse(),
            )

        val currentBeforeMerge = login(sourceId, "current", source)
        val anotherDevice = login(sourceId, "other", source)
        val unaffected = login(retainedId, "remaining", remaining)
        service.mergeAndApprove(retainedId, sourceId, 99)
        val current = rotate(currentBeforeMerge)

        assertThatThrownBy { accounts.unlink(retainedId, source, sourceId) }
            .isInstanceOf(MemberAccountException::class.java)
        assertThat(identities.isValid(retainedId, source)).isTrue()
        accounts.unlink(retainedId, provider.getSessionIdentity(current.accessToken), sourceId)

        assertThat(identities.isValid(retainedId, source)).isFalse()
        assertThat(resolver.resolve(provider.generateAccessToken(retainedId.toString(), source))).isNull()
        assertThatThrownBy { rotate(anotherDevice) }.isInstanceOf(TokenInvalidException::class.java)
        assertThat(provider.getMemberId(rotate(unaffected).accessToken)).isEqualTo(retainedId)

        val pool = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val successors =
            try {
                (1..2).map {
                    pool.submit(
                        Callable {
                            ready.countDown()
                            check(ready.await(10, TimeUnit.SECONDS))
                            rotate(current.refreshToken)
                        },
                    )
                }.map { it.get(20, TimeUnit.SECONDS) }
            } finally {
                pool.shutdownNow()
            }
        assertThat(successors.map { it.refreshToken }.distinct()).hasSize(1)
        val next = rotate(successors.first().refreshToken)
        assertThat(resolver.resolve(next.accessToken)?.memberId).isEqualTo(retainedId)

        TransactionTemplate(tx).executeWithoutResult { tokenStore.deleteByMemberId(retainedId) }
        assertThat(resolver.resolve(next.accessToken)).isNull()
        assertThatThrownBy { rotate(next.refreshToken) }.isInstanceOf(TokenNotFoundException::class.java)
    }

    private fun assertCleanupAndAudit() {
        assertThat(jdbc.queryForList("select member_id from refresh_tokens", Long::class.java)).containsExactly(1L, 1L)
        assertThat(jdbc.queryForList("select member_id from member_oauth_link_requests", Long::class.java)).containsExactly(1L)
        assertThat(jdbc.queryForMap("select actor_member_id, retained_member_id, source_member_id from member_merge_events"))
            .containsEntry("actor_member_id", 99L).containsEntry("retained_member_id", 1L).containsEntry("source_member_id", 2L)
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["core.entity"])
    @EnableJpaRepositories(basePackages = ["core.persistence"])
    @Import(
        core.application.member.application.service.MemberActivityService::class, core.persistence.member.repository.MemberActivityRepository::class,
        JooqDslConfig::class,
        MemberRepository::class,
        MemberMergeRepository::class, MemberAccountRepository::class, RefreshTokenRepository::class,
        MemberRoleRepository::class,
        MemberTeamRepository::class,
        MemberCohortRepository::class,
        MemberApprovalService::class,
        MemberAccountService::class,
        MemberMergeService::class,
        MemberIdentityLockService::class,
        core.application.security.oauth.token.LoginIdentityValidator::class,
        core.persistence.member.repository.oauth.MemberOAuthRepository::class,
        core.persistence.membercredential.repository.MemberCredentialRepository::class,
    )
    class Config

    companion object {
        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
