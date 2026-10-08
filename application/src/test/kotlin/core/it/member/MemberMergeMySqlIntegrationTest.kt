package core.it.member

import core.application.common.configuration.JooqDslConfig
import core.application.member.application.exception.InvalidMemberMergeException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberOAuthConflictException
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberIdentityLockService
import core.application.member.application.service.MemberMergeService
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = [MemberMergeMySqlIntegrationTest.Config::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemberMergeMySqlIntegrationTest {
    @Autowired lateinit var service: MemberMergeService

    @Autowired lateinit var locks: MemberIdentityLockService

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var tx: PlatformTransactionManager

    @MockitoBean lateinit var cohorts: CohortQueryUseCase

    @MockitoBean lateinit var roles: RoleQueryUseCase

    @MockitoBean lateinit var initializer: MemberActivationInitializer

    @BeforeEach
    fun fixture() {
        listOf("member_credentials", "member_oauth", "member_roles", "member_teams", "member_cohorts", "members", "roles", "teams", "cohorts").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'GUEST')")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (1, 1, 19, 0, 0)")
        (1L..2L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at) values (?, ?, ?, 'SERVER', 'PENDING', now(6))", id, "member-$id", "member-$id@example.test")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
            jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, 2, 19, now(6))", id)
            jdbc.update("insert into member_teams (member_id, team_id) values (?, 1)", id)
            jdbc.update("insert into member_oauth (member_oauth_id, member_id, external_id, provider, email) values (?, ?, ?, ?, ?)", id, id, "external-$id", if (id == 1L) "KAKAO" else "APPLE", "oauth-$id@example.test")
        }
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(roles.findIdByName("DEEPER")).thenReturn(1)
    }

    @Test
    fun `원본 연결을 보존하고 OAuth ID를 이전하며 유지 계정을 승인한다`() {
        service.mergeAndApprove(1, 2)
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
        assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(InvalidMemberMergeException::class.java)
    }

    @Test
    fun `승인 초기화 실패는 OAuth 이전과 원본 삭제까지 취소한다`() {
        doThrow(IllegalStateException("initialization failed")).`when`(initializer).initialize(MemberId(1), CohortId(19))
        assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(IllegalStateException::class.java)
        assertUnchanged()
    }

    @Test
    fun `동일 provider 충돌은 변경 없이 거절한다`() {
        jdbc.update("update member_oauth set provider = 'KAKAO' where member_id = 2")
        assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(MemberOAuthConflictException::class.java)
        assertUnchanged()
    }

    @Test
    fun `어느 계정이든 비밀번호 정보가 있으면 거절한다`() {
        listOf(1L, 2L).forEach { id ->
            jdbc.update("insert into member_credentials (member_id, email, password, created_at, updated_at) values (?, ?, 'test-hash', now(6), now(6))", id, "password-$id@example.test")
            assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(InvalidMemberMergeException::class.java)
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
                            runCatching { service.mergeAndApprove(retained, source) }
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
                service.mergeAndApprove(1, 2)
            }
            assertThat(future.get(20, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(MemberDeletedException::class.java)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun assertUnchanged() {
        assertThat(jdbc.queryForObject("select count(*) from members where status = 'PENDING' and deleted_at is null", Int::class.java)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select member_id from member_oauth where member_oauth_id = 2", Long::class.java)).isEqualTo(2)
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["core.entity"])
    @EnableJpaRepositories(basePackages = ["core.persistence"])
    @Import(
        JooqDslConfig::class,
        MemberRepository::class,
        MemberMergeRepository::class,
        MemberRoleRepository::class,
        MemberTeamRepository::class,
        MemberCohortRepository::class,
        MemberApprovalService::class,
        MemberMergeService::class,
        MemberIdentityLockService::class,
    )
    class Config

    companion object {
        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
