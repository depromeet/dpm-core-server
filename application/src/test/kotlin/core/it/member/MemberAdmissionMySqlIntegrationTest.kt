package core.it.member

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.common.exception.BusinessException
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberAdmissionService
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.cohort.MemberCohortService
import core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.authorization.repository.RoleRepository
import core.persistence.member.repository.MemberAdmissionEventRepository
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
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
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
import java.util.concurrent.atomic.AtomicLong

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(
    MemberRepository::class,
    MemberAdmissionEventRepository::class,
    MemberAdmissionService::class,
    MemberApprovalService::class,
    MemberRoleRepository::class,
    MemberTeamRepository::class,
    MemberCohortRepository::class,
    MemberCohortService::class,
    RoleRepository::class,
)
class MemberAdmissionMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var service: MemberAdmissionService

    @Autowired lateinit var approvals: MemberApprovalService

    @Autowired lateinit var members: MemberRepository

    @Autowired lateinit var roles: RoleRepository

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var initializer: MemberActivationInitializer

    @MockitoBean lateinit var roleQueries: RoleQueryUseCase

    @MockitoBean lateinit var memberQueries: MemberQueryUseCase

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @MockitoBean lateinit var sessionFeedbackForms: SessionFeedbackFormCommandService

    @BeforeEach
    fun fixture() {
        listOf("member_admission_events", "role_permissions", "member_roles", "member_teams", "member_cohorts", "member_permissions", "member_oauth", "members", "permissions", "roles", "teams", "cohorts").forEach {
            jdbc.execute("delete from $it")
        }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (18, '18', false, 0, 0), (19, '19', true, 0, 0)")
        (1L..3L).forEach {
            jdbc.update("insert into members (member_id, name, signup_email, status, created_at, updated_at) values (?, '홍길동', ?, 'PENDING', now(6), now(6))", it, "signup$it@example.com")
        }
        jdbc.update("insert into member_cohorts (member_id, cohort_id) values (2, 19), (3, 18)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'ORGANIZER'), (3, 'MASTER')")
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
    }

    @Test
    fun `반려는 목록에서 제외하고 재신청해도 이력을 보존하며 반복 재신청은 변경하지 않는다`() {
        service.reject(1)
        service.reject(2)
        assertThat(members.findManagementMembers(19)).isEmpty()
        assertThatThrownBy { service.reject(2) }.isInstanceOf(BusinessException::class.java)
        assertThatThrownBy { service.reject(3) }.isInstanceOf(BusinessException::class.java)
        service.reapply(1)
        val updated = jdbc.queryForObject("select updated_at from members where member_id = 1", java.sql.Timestamp::class.java)
        service.reapply(1)
        assertThat(jdbc.queryForObject("select updated_at from members where member_id = 1", java.sql.Timestamp::class.java)).isEqualTo(updated)
        assertThat(events(1)).containsExactly("REJECTED", "REAPPLIED")
        assertThat(members.findManagementMembers(19).map { it.memberId }).containsExactly(1)
    }

    @Test
    fun `기수 변경 뒤 재신청은 과거 소속과 이력을 보존하고 현재 기수 목록에 나타난다`() {
        service.reject(1)
        service.reject(2)
        jdbc.update("update cohorts set is_active = false")
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (20, '20', true, 0, 0)")
        listOf(1L, 2L).forEach {
            service.reapply(it)
            assertThat(status(it)).isEqualTo("PENDING")
            assertThat(events(it)).containsExactly("REJECTED", "REAPPLIED")
        }
        assertThat(jdbc.queryForList("select cohort_id from member_cohorts where member_id = 1 order by cohort_id", Long::class.java)).containsExactly(20)
        assertThat(jdbc.queryForList("select cohort_id from member_cohorts where member_id = 2 order by cohort_id", Long::class.java)).containsExactly(19, 20)
        assertThat(members.findManagementMembers(20).map { it.memberId }).containsExactlyInAnyOrder(1, 2)
        jdbc.update("update cohorts set is_active = false")
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (21, '21', true, 0, 0)")
        service.reapply(2)
        assertThat(events(2)).containsExactly("REJECTED", "REAPPLIED")
        assertThat(jdbc.queryForList("select cohort_id from member_cohorts where member_id = 2 order by cohort_id", Long::class.java)).containsExactly(19, 20)
    }

    @Test
    fun `활성 표시가 없으면 기존 승인과 동일하게 최신 기수를 사용하고 기수가 전혀 없으면 거절한다`() {
        service.reject(1)
        jdbc.update("update cohorts set is_active = false")
        service.reapply(1)
        assertThat(jdbc.queryForList("select cohort_id from member_cohorts where member_id = 1", Long::class.java)).containsExactly(19)
        service.reject(1)
        jdbc.execute("delete from member_cohorts")
        jdbc.execute("delete from cohorts")
        assertThatThrownBy { service.reapply(1) }.isInstanceOf(CohortNotFoundException::class.java)
        assertThat(status(1)).isEqualTo("REJECTED")
        assertThat(events(1)).containsExactly("REJECTED", "REAPPLIED", "REJECTED")
    }

    @Test
    fun `상태 저장이 실패하면 반려와 재신청 이력도 롤백한다`() {
        jdbc.execute("create trigger member622_reject_update before update on members for each row signal sqlstate '45000' set message_text = 'admission rollback test'")
        try {
            assertThatThrownBy { service.reject(1) }.isInstanceOf(RuntimeException::class.java)
            assertThat(events(1)).isEmpty()
            assertThat(status(1)).isEqualTo("PENDING")
        } finally {
            jdbc.execute("drop trigger member622_reject_update")
        }
        service.reject(1)
        jdbc.execute("create trigger member622_reject_update before update on members for each row signal sqlstate '45000' set message_text = 'admission rollback test'")
        try {
            assertThatThrownBy { service.reapply(1) }.isInstanceOf(RuntimeException::class.java)
            assertThat(events(1)).containsExactly("REJECTED")
            assertThat(status(1)).isEqualTo("REJECTED")
            assertThat(jdbc.queryForObject("select count(*) from member_cohorts where member_id = 1", Int::class.java)).isZero()
        } finally {
            jdbc.execute("drop trigger member622_reject_update")
        }
    }

    @Test
    fun `승인과 반려는 같은 회원 잠금을 기다리고 두 처리가 동시에 성공하지 않는다`() {
        race({ service.reject(1) }, { approvals.approve(listOf(1)) })
        assertThat(status(1)).isEqualTo("REJECTED")
        assertThat(events(1)).containsExactly("REJECTED")
        race({ approvals.approve(listOf(2)) }, { service.reject(2) })
        assertThat(status(2)).isEqualTo("ACTIVE")
        assertThat(events(2)).isEmpty()
    }

    @Test
    fun `동시 재신청은 이력을 한 번만 기록한다`() {
        service.reject(1)
        race({ service.reapply(1) }, { service.reapply(1) }, secondSucceeds = true)
        assertThat(events(1)).containsExactly("REJECTED", "REAPPLIED")
        assertThat(status(1)).isEqualTo("PENDING")
    }

    @Test
    fun `반려와 대기 상태는 잔존 운영진 및 마스터 권한으로 승인 기능을 사용할 수 없다`() {
        jdbc.update("insert into permissions (permission_id, resource, action) values (1, 'member', 'create')")
        jdbc.update("insert into role_permissions (role_id, permission_id, granted_at) values (2, 1, now(6)), (3, 1, now(6))")
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (2, 2, 19, now(6)), (2, 3, null, now(6))")
        val names = listOf("ORGANIZER", "MASTER")
        assertThat(roles.findAllPermissionsByMemberIdAndRoleNames(MemberId(2), names)).isEmpty()
        service.reject(2)
        assertThat(roles.findAllPermissionsByMemberIdAndRoleNames(MemberId(2), names)).isEmpty()
        service.reapply(2)
        assertThat(roles.findAllPermissionsByMemberIdAndRoleNames(MemberId(2), names)).isEmpty()
        listOf("ACTIVE", "INACTIVE").forEach {
            jdbc.update("update members set status = ? where member_id = 2", it)
            assertThat(roles.findAllPermissionsByMemberIdAndRoleNames(MemberId(2), names)).containsExactly("create:member")
        }
    }

    private fun race(
        firstAction: () -> Unit,
        secondAction: () -> Unit,
        secondSucceeds: Boolean = false,
    ) {
        val changed = CountDownLatch(1)
        val release = CountDownLatch(1)
        val connectionId = AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            firstAction()
                            connectionId.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            changed.countDown()
                            check(release.await(15, TimeUnit.SECONDS))
                        }
                    },
                )
            check(changed.await(15, TimeUnit.SECONDS))
            val second = pool.submit(Callable { runCatching { secondAction() } })
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            var waiting = false
            while (System.nanoTime() < deadline) {
                waiting = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w join performance_schema.threads t on t.thread_id = w.blocking_thread_id where t.processlist_id = ?", Long::class.java, connectionId.get())!! > 0
                if (waiting) break
                CountDownLatch(1).await(20, TimeUnit.MILLISECONDS)
            }
            assertThat(waiting).isTrue()
            release.countDown()
            first.get(15, TimeUnit.SECONDS)
            val outcome = second.get(15, TimeUnit.SECONDS)
            if (secondSucceeds) {
                assertThat(outcome.isSuccess).isTrue()
            } else {
                assertThat(outcome.exceptionOrNull()).isInstanceOf(BusinessException::class.java)
            }
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    private fun status(id: Long): String = jdbc.queryForObject("select status from members where member_id = ?", String::class.java, id)!!

    private fun events(id: Long): List<String> = jdbc.queryForList("select event_type from member_admission_events where member_id = ? order by member_admission_event_id", String::class.java, id)

    companion object {
        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
