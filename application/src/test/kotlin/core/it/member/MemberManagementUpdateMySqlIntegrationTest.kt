package core.it.member

import core.application.authorization.application.service.RoleCommandService
import core.application.authorization.presentation.request.UpdateMemberRoleRequest
import core.application.member.application.exception.InvalidMemberManagementTeamException
import core.application.member.application.exception.MemberManagementTargetNotAllowedException
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.application.service.role.MemberRoleService
import core.application.member.presentation.request.MemberManagementBulkUpdateRequest
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import jakarta.persistence.EntityManager
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
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MemberRepository::class, MemberRoleRepository::class, MemberTeamRepository::class, MemberManagementCommandService::class, RoleCommandService::class, MemberRoleService::class, MemberCohortRepository::class, CurrentCohortRoleResolver::class)
class MemberManagementUpdateMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var service: MemberManagementCommandService

    @Autowired lateinit var legacyRoleService: RoleCommandService

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @Autowired lateinit var entityManager: EntityManager

    @MockitoBean lateinit var memberQueryService: MemberQueryService

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @MockitoBean lateinit var roleQueries: RoleQueryUseCase

    @BeforeEach
    fun fixture() {
        listOf("member_teams", "member_roles", "member_cohorts", "member_permissions", "member_oauth", "members", "teams", "roles", "cohorts").forEach {
            jdbc.execute("delete from $it")
        }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (18, '18', false, 0, 0), (19, '19', true, 0, 0), (20, '20', false, 0, 0)")
        (1L..6L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at, updated_at) values (?, ?, ?, 'WEB', 'ACTIVE', '2020-01-01 00:00:00', '2020-01-01 00:00:00')", id, "nickname$id", "signup$id@example.com")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, ?)", id, if (id == 3L) 18 else 19)
        }
        jdbc.update("insert into member_cohorts (member_id, cohort_id) values (1, 18), (2, 18), (1, 19)")
        jdbc.update("update members set status = 'INACTIVE' where member_id = 2")
        jdbc.update("update members set status = 'PENDING' where member_id = 4")
        jdbc.update("update members set deleted_at = '2020-01-02 00:00:00' where member_id = 5")
        jdbc.update("update members set status = 'WITHDRAWN' where member_id = 6")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (180, 1, 18, 0, 0), (191, 1, 19, 0, 0), (192, 2, 19, 0, 0)")
        jdbc.update("insert into member_teams (member_id, team_id) values (1, 180), (1, 191), (2, 180), (2, 191)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'ORGANIZER'), (3, 'CORE'), (4, 'MASTER'), (5, 'GUEST')")
        listOf(1L, 2L).forEach { id ->
            addRole(id, 1, 19)
            addRole(id, 2, 18)
            addRole(id, 4, null)
            addRole(id, 5, null)
        }
        addRole(1, 3, null) // 과거 데이터의 기수 없는 타입도 현재 resolver에서 유효하다.
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
        `when`(roleQueries.findIdByName("ORGANIZER")).thenReturn(2)
        `when`(roleQueries.findIdByName("CORE")).thenReturn(3)
        `when`(memberQueryService.getMemberById(MemberId(1))).thenAnswer { members.findById(MemberId(1))!! }
    }

    @Test
    fun `네 컬럼을 함께 수정하고 과거 기수와 시스템 권한을 보존한다`() {
        val before = updatedAt(1)
        service.update(1, MemberManagementUpdateRequest("SERVER", 192, "ORGANIZER", "INACTIVE"))

        val row = jdbc.queryForMap("select name, signup_email, part, status from members where member_id = 1")
        assertThat(row).containsEntry("name", "nickname1").containsEntry("signup_email", "signup1@example.com")
            .containsEntry("part", "SERVER").containsEntry("status", "INACTIVE")
        assertThat(teamIds(1)).containsExactlyInAnyOrder(180, 192)
        assertThat(activeRoles(1)).containsExactlyInAnyOrder("2:18", "2:19", "4:null", "5:null")
        assertThat(updatedAt(1)).isAfter(before)
        assertThat(members.findManagementMembers(19).single { it.memberId == 1L }.updatedAt)
            .isBetween(Instant.now().minusSeconds(30), Instant.now().plusSeconds(1))
        assertThat(jdbc.queryForObject("select count(*) from member_cohorts where member_id = 1", Int::class.java)).isEqualTo(3)
    }

    @Test
    fun `미배정으로 해제하고 같은 값 재요청은 이력과 갱신 시각을 바꾸지 않는다`() {
        service.update(1, MemberManagementUpdateRequest("UNASSIGNED", 0, "UNASSIGNED"))
        assertThat(jdbc.queryForMap("select part, status from members where member_id = 1"))
            .containsEntry("part", null).containsEntry("status", "ACTIVE")
        assertThat(teamIds(1)).containsExactly(180)
        assertThat(activeRoles(1)).containsExactlyInAnyOrder("2:18", "4:null", "5:null")
        val at = updatedAt(1)
        val roleRows = jdbc.queryForObject("select count(*) from member_roles where member_id = 1", Int::class.java)
        service.update(1, MemberManagementUpdateRequest("UNASSIGNED", 0, "UNASSIGNED"))
        assertThat(updatedAt(1)).isEqualTo(at)
        assertThat(jdbc.queryForObject("select count(*) from member_roles where member_id = 1", Int::class.java)).isEqualTo(roleRows)
    }

    @Test
    fun `팀 타입만 변경한 갱신 시각과 컬럼별 일괄 수정 결과를 제공한다`() {
        val before = updatedAt(2)
        service.updateBulk(MemberManagementBulkUpdateRequest(listOf(2, 1), MemberManagementUpdateRequest(teamId = 192)))
        assertThat(updatedAt(2)).isAfter(before)
        listOf(1L, 2L).forEach { id ->
            assertThat(teamIds(id)).containsExactlyInAnyOrder(180, 192)
            assertThat(jdbc.queryForObject("select part from members where member_id = ?", String::class.java, id)).isEqualTo("WEB")
        }
        val afterTeam = updatedAt(2)
        service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1, 2), MemberManagementUpdateRequest(memberType = "CORE")))
        assertThat(updatedAt(2)).isAfterOrEqualTo(afterTeam)
        listOf(1L, 2L).forEach { id -> assertThat(activeRoles(id)).containsExactlyInAnyOrder("2:18", "3:19", "4:null", "5:null") }
        service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1, 2), MemberManagementUpdateRequest(status = "ACTIVE")))
        assertThat(jdbc.queryForObject("select status from members where member_id = 2", String::class.java)).isEqualTo("ACTIVE")
    }

    @Test
    fun `이전 기수 대기 탈퇴 삭제 누락 대상과 다른 기수 팀은 전체 변경 전에 거절한다`() {
        listOf(3L, 4L, 5L, 6L, 999L).forEach { invalidId ->
            assertThatThrownBy {
                service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1, invalidId), MemberManagementUpdateRequest(part = "SERVER")))
            }.isInstanceOf(MemberManagementTargetNotAllowedException::class.java)
        }
        assertThatThrownBy { service.update(1, MemberManagementUpdateRequest(part = "SERVER", teamId = 180)) }
            .isInstanceOf(InvalidMemberManagementTeamException::class.java)
        assertThat(jdbc.queryForObject("select part from members where member_id = 1", String::class.java)).isEqualTo("WEB")
        assertThat(teamIds(1)).containsExactlyInAnyOrder(180, 191)
    }

    @Test
    fun `팀과 타입 변경 후 실패하면 전체 트랜잭션을 롤백한다`() {
        val originalRoles = activeRoles(1)
        val originalAt = updatedAt(1)
        jdbc.execute("create trigger member616_reject_update before update on members for each row signal sqlstate '45000' set message_text = 'member616 rollback test'")
        try {
            assertThatThrownBy { service.update(1, MemberManagementUpdateRequest("SERVER", 192, "ORGANIZER", "INACTIVE")) }
                .isInstanceOf(RuntimeException::class.java)
        } finally {
            jdbc.execute("drop trigger member616_reject_update")
        }
        assertThat(teamIds(1)).containsExactlyInAnyOrder(180, 191)
        assertThat(activeRoles(1)).containsExactlyInAnyOrderElementsOf(originalRoles)
        assertThat(updatedAt(1)).isEqualTo(originalAt)
    }

    @Test
    fun `JPA와 jOOQ 변경은 같은 Spring 트랜잭션에서 롤백한다`() {
        assertThatThrownBy {
            TransactionTemplate(transactionManager).executeWithoutResult {
                members.save(Member(MemberId(1), "changed", signupEmail = "signup1@example.com", part = MemberPart.WEB, status = MemberStatus.ACTIVE))
                entityManager.flush()
                service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1), MemberManagementUpdateRequest(part = "SERVER")))
                throw IllegalStateException("rollback")
            }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(jdbc.queryForMap("select name, part from members where member_id = 1"))
            .containsEntry("name", "nickname1").containsEntry("part", "WEB")
    }

    @Test
    fun `동시 수정은 기수별 팀 타입을 하나로 유지하고 다른 컬럼을 덮어쓰지 않는다`() {
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val futures =
                (0 until 8).map { index ->
                    pool.submit(
                        Callable {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            val ids = if (index % 2 == 0) listOf(2L, 1L) else listOf(1L, 2L)
                            val changes =
                                when (index % 4) {
                                    0 -> MemberManagementUpdateRequest(part = "SERVER")
                                    1 -> MemberManagementUpdateRequest(teamId = 192)
                                    2 -> MemberManagementUpdateRequest(memberType = "ORGANIZER")
                                    else -> MemberManagementUpdateRequest(status = "INACTIVE")
                                }
                            service.updateBulk(MemberManagementBulkUpdateRequest(ids, changes))
                        },
                    )
                }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            futures.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
        listOf(1L, 2L).forEach { id ->
            assertThat(jdbc.queryForMap("select part, status from members where member_id = ?", id))
                .containsEntry("part", "SERVER").containsEntry("status", "INACTIVE")
            assertThat(teamIds(id)).containsExactlyInAnyOrder(180, 192)
            assertThat(activeRoles(id)).containsExactlyInAnyOrder("2:18", "2:19", "4:null", "5:null")
        }
    }

    @Test
    fun `v3에서 CORE로 변경한 회원도 기존 역할 API에서 다른 타입으로 교체한다`() {
        service.update(1, MemberManagementUpdateRequest(memberType = "CORE"))
        legacyRoleService.updateMemberRole(MemberId(1), UpdateMemberRoleRequest("DEEPER", 19))
        assertThat(activeRoles(1)).containsExactlyInAnyOrder("1:19", "2:18", "4:null", "5:null")
    }

    @Test
    fun `기존 역할 API는 v3 변경의 잠금을 기다린 뒤 최신 타입 한 개를 교체한다`() {
        runOrderedRoleChanges(
            first = { service.update(1, MemberManagementUpdateRequest(memberType = "CORE")) },
            second = { legacyRoleService.updateMemberRole(MemberId(1), UpdateMemberRoleRequest("DEEPER", 19)) },
        )
        assertThat(activeRoles(1)).containsExactlyInAnyOrder("1:19", "2:18", "4:null", "5:null")
    }

    @Test
    fun `v3 수정도 기존 역할 API 트랜잭션 이후 최신 타입으로 교체한다`() {
        runOrderedRoleChanges(
            first = { legacyRoleService.updateMemberRole(MemberId(1), UpdateMemberRoleRequest("ORGANIZER", 19)) },
            second = { service.update(1, MemberManagementUpdateRequest(memberType = "DEEPER")) },
        )
        assertThat(activeRoles(1)).containsExactlyInAnyOrder("1:19", "2:18", "4:null", "5:null")
    }

    /** 첫 쓰기의 커밋을 보류하고 MySQL이 두 번째 쓰기를 실제로 잠금 대기시키는지 확인한다. */
    private fun runOrderedRoleChanges(
        first: () -> Unit,
        second: () -> Unit,
    ) {
        val changed = CountDownLatch(1)
        val release = CountDownLatch(1)
        val connectionId = AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val firstResult =
                pool.submit(
                    Callable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            first()
                            connectionId.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            changed.countDown()
                            check(release.await(10, TimeUnit.SECONDS)) { "첫 역할 변경 트랜잭션 해제 시간 초과" }
                        }
                    },
                )
            check(changed.await(10, TimeUnit.SECONDS)) { "첫 역할 변경 준비 시간 초과" }
            val secondResult = pool.submit(Callable { second() })
            awaitDatabaseLockWait(connectionId.get())
            release.countDown()
            firstResult.get(10, TimeUnit.SECONDS)
            secondResult.get(10, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    private fun awaitDatabaseLockWait(blockingConnectionId: Long) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val count =
                jdbc.queryForObject(
                    "select count(*) from performance_schema.data_lock_waits w join performance_schema.threads t on t.thread_id = w.blocking_thread_id where t.processlist_id = ?",
                    Long::class.java,
                    blockingConnectionId,
                )!!
            if (count > 0) return
            CountDownLatch(1).await(20, TimeUnit.MILLISECONDS)
        }
        error("두 번째 역할 변경이 첫 트랜잭션의 잠금을 기다리지 않았습니다")
    }

    private fun teamIds(memberId: Long): List<Long> = jdbc.queryForList("select team_id from member_teams where member_id = ?", Long::class.java, memberId)

    private fun activeRoles(memberId: Long): List<String> =
        jdbc.queryForList("select role_id, cohort_id from member_roles where member_id = ? and deleted_at is null", memberId)
            .map { "${it["role_id"]}:${it["cohort_id"]}" }

    private fun updatedAt(memberId: Long): Instant = jdbc.queryForObject("select updated_at from members where member_id = ?", java.sql.Timestamp::class.java, memberId)!!.toInstant()

    private fun addRole(
        memberId: Long,
        roleId: Long,
        cohortId: Long?,
    ) {
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, ?, ?, now(6))", memberId, roleId, cohortId)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
