package core.it.member

import core.application.afterParty.application.service.AfterPartyCommandService
import core.application.afterParty.application.service.invitee.AfterPartyInviteeCommandService
import core.application.announcement.application.service.AnnouncementCommandService
import core.application.announcement.application.service.AnnouncementReadCommandService
import core.application.announcement.application.service.AssignmentSubmissionCommandService
import core.application.member.application.exception.MemberApprovalTargetNotAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberCommandService
import core.application.member.application.service.MemberLoginEmailResolver
import core.application.member.application.service.MemberNameHashTypeValidator
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.access.MemberAccessService
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.controller.MemberApprovalController
import core.application.member.presentation.controller.MemberController
import core.application.member.presentation.request.MemberApprovalRequest
import core.application.member.presentation.request.WhiteListCheckRequest
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.afterParty.port.inbound.AfterPartyInviteTagQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyQueryUseCase
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.port.inbound.AnnouncementAssignmentCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.notification.port.inbound.SentAnnouncementNotificationCommandUseCase
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.afterParty.repository.AfterPartyInviteTagRepository
import core.persistence.afterParty.repository.AfterPartyRepository
import core.persistence.afterParty.repository.invitee.AfterPartyInviteeRepository
import core.persistence.announcement.repository.AnnouncementReadRepository
import core.persistence.announcement.repository.AssignmentSubmissionRepository
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
import org.mockito.Mockito.mock
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
@Import(
    MemberRepository::class,
    MemberRoleRepository::class,
    MemberTeamRepository::class,
    MemberCohortRepository::class,
    MemberApprovalService::class,
    MemberActivationInitializer::class,
    MemberQueryService::class,
    MemberAccessService::class,
    CurrentCohortRoleResolver::class,
    AnnouncementCommandService::class,
    AnnouncementReadCommandService::class,
    AssignmentSubmissionCommandService::class,
    AnnouncementReadRepository::class,
    AssignmentSubmissionRepository::class,
    AfterPartyCommandService::class,
    AfterPartyInviteeCommandService::class,
    AfterPartyRepository::class,
    AfterPartyInviteTagRepository::class,
    AfterPartyInviteeRepository::class,
)
class MemberApprovalMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var service: MemberApprovalService

    @Autowired lateinit var queryService: MemberQueryService

    @Autowired lateinit var sessions: SessionPersistencePort

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var roleQueryUseCase: RoleQueryUseCase

    @MockitoBean lateinit var memberOAuthService: MemberOAuthService

    @MockitoBean lateinit var memberLoginEmailResolver: MemberLoginEmailResolver

    @MockitoBean lateinit var announcementPersistencePort: AnnouncementPersistencePort

    @MockitoBean lateinit var announcementAssignmentCommandUseCase: AnnouncementAssignmentCommandUseCase

    @MockitoBean lateinit var assignmentPersistencePort: AssignmentPersistencePort

    @MockitoBean lateinit var announcementQueryUseCase: AnnouncementQueryUseCase

    @MockitoBean lateinit var announcementReadQueryUseCase: AnnouncementReadQueryUseCase

    @MockitoBean lateinit var assignmentQueryUseCase: AssignmentQueryUseCase

    @MockitoBean lateinit var sentAnnouncementNotificationCommandUseCase: SentAnnouncementNotificationCommandUseCase

    @MockitoBean lateinit var afterPartyQueryUseCase: AfterPartyQueryUseCase

    @MockitoBean lateinit var afterPartyInviteTagQueryUseCase: AfterPartyInviteTagQueryUseCase

    @MockitoBean lateinit var afterPartyInviteeQueryUseCase: AfterPartyInviteeQueryUseCase

    @MockitoBean lateinit var notificationCommandUseCase: NotificationCommandUseCase

    @MockitoBean lateinit var sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase

    @BeforeEach
    fun fixture() {
        listOf(
            "after_party_invitees", "after_party_invite_tags", "after_party", "assignment_submissions", "announcement_reads",
            "attendances", "sessions", "member_teams", "member_roles", "member_cohorts", "member_permissions", "member_oauth",
            "members", "teams", "roles", "cohorts",
        ).forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (18, '18', false, 0, 0), (19, '19', true, 0, 0)")
        (1L..6L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at, updated_at) values (?, ?, ?, 'SERVER', 'PENDING', '2020-01-01 00:00:00', '2020-01-01 00:00:00')", id, "nickname", "signup$id@example.com")
        }
        jdbc.update("insert into member_cohorts (member_id, cohort_id) values (2, 19), (2, 18), (3, 18), (4, 19), (5, 19)")
        jdbc.update("update members set status = 'ACTIVE' where member_id = 4")
        jdbc.update("update members set status = 'INACTIVE' where member_id = 5")
        jdbc.update("update members set deleted_at = now(6) where member_id = 6")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (180, 1, 18, 0, 0), (191, 1, 19, 0, 0)")
        jdbc.update("insert into member_teams (member_id, team_id) values (2, 180), (2, 191), (4, 191), (5, 191)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'ORGANIZER'), (3, 'GUEST')")
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (1, 3, null, now(6)), (2, 2, 19, now(6)), (4, 2, 19, now(6)), (5, 2, 19, now(6))")
        `when`(roleQueryUseCase.findIdByName("DEEPER")).thenReturn(1)
        `when`(announcementQueryUseCase.getAll()).thenReturn(
            listOf(Announcement(AnnouncementId(1), AnnouncementType.GENERAL, "notice", null, MemberId(4))),
        )
        `when`(assignmentQueryUseCase.getAllAssignments()).thenReturn(
            listOf(Assignment(AssignmentId(1), SubmitType.INDIVIDUAL, null, null, null)),
        )
        val at = Instant.parse("2026-10-01T00:00:00Z")
        TransactionTemplate(transactionManager).executeWithoutResult {
            sessions.save(Session(cohortId = CohortId(19), date = at, week = 1, place = "online", eventName = "session", attendancePolicy = AttendancePolicy(at, at.plusSeconds(600), at.plusSeconds(1200), "1234")))
        }
        jdbc.update("insert into after_party (after_party_id, title, category, scheduled_at, closed_at, is_approved, member_id, created_at, updated_at, can_edit_after_approval) values (1, 'party', 'AFTER_PARTY', '2099-01-01', '2099-01-01', false, 4, now(6), now(6), false)")
        jdbc.update("insert into after_party_invite_tags (after_party_id, cohort_id, authority_id, tag_name, created_at) values (1, 19, 1, '19기 디퍼', now(6))")
    }

    @Test
    fun `무소속 현재 기수 대기자를 승인하고 출석 공지 과제 회식을 응답 전에 초기화한다`() {
        service.approve(listOf(2, 1))
        listOf(1L, 2L).forEach { id ->
            assertThat(jdbc.queryForMap("select name, signup_email, part, status from members where member_id = ?", id))
                .containsEntry("name", "nickname").containsEntry("signup_email", "signup$id@example.com")
                .containsEntry("part", "SERVER").containsEntry("status", "ACTIVE")
            assertThat(count("member_cohorts", id)).isEqualTo(if (id == 2L) 2 else 1)
            assertThat(count("member_teams", id)).isEqualTo(if (id == 2L) 1 else 0)
            assertThat(jdbc.queryForObject("select count(*) from member_roles where member_id = ? and cohort_id = 19 and role_id = 1 and deleted_at is null", Int::class.java, id)).isEqualTo(1)
            assertInitialized(id)
            assertThat(jdbc.queryForObject("select team_id from assignment_submissions where member_id = ?", Long::class.java, id)).isZero()
        }
        assertThat(jdbc.queryForObject("select count(*) from member_roles where member_id = 1 and role_id = 3 and deleted_at is null", Int::class.java)).isEqualTo(1)
    }

    @Test
    fun `승인 재요청은 상태 팀 역할 시각과 초기화 기록을 바꾸지 않는다`() {
        service.approve(listOf(1))
        val before = jdbc.queryForObject("select updated_at from members where member_id = 1", java.sql.Timestamp::class.java)
        service.approve(listOf(1, 4, 5))
        assertInitialized(1)
        assertThat(jdbc.queryForObject("select updated_at from members where member_id = 1", java.sql.Timestamp::class.java)).isEqualTo(before)
        assertThat(jdbc.queryForObject("select status from members where member_id = 5", String::class.java)).isEqualTo("INACTIVE")
        listOf(4L, 5L).forEach {
            assertThat(count("member_teams", it)).isEqualTo(1)
            assertThat(count("attendances", it)).isZero()
            assertThat(jdbc.queryForObject("select role_id from member_roles where member_id = ? and deleted_at is null", Long::class.java, it)).isEqualTo(2)
        }
    }

    @Test
    fun `유효하지 않은 대상을 섞으면 다른 대기자도 승인하지 않는다`() {
        assertThatThrownBy { service.approve(listOf(1, 3)) }.isInstanceOf(MemberApprovalTargetNotAllowedException::class.java)
        assertThatThrownBy { service.approve(listOf(1, 6)) }.isInstanceOf(MemberDeletedException::class.java)
        assertThatThrownBy { service.approve(listOf(1, 999)) }.isInstanceOf(MemberNotFoundException::class.java)
        assertThat(jdbc.queryForObject("select status from members where member_id = 1", String::class.java)).isEqualTo("PENDING")
        assertThat(count("member_cohorts", 1)).isZero()
        assertThat(count("attendances", 1)).isZero()
    }

    @Test
    fun `두 번째 회원의 회식 저장이 실패하면 첫 회원과 모든 초기화도 롤백한다`() {
        jdbc.execute("create trigger member618_reject_invitee before insert on after_party_invitees for each row begin if new.member_id = 2 then signal sqlstate '45000' set message_text = 'member618 rollback test'; end if; end")
        try {
            assertThatThrownBy { service.approve(listOf(1, 2)) }.isInstanceOf(RuntimeException::class.java)
        } finally {
            jdbc.execute("drop trigger member618_reject_invitee")
        }
        listOf(1L, 2L).forEach { id ->
            assertThat(jdbc.queryForObject("select status from members where member_id = ?", String::class.java, id)).isEqualTo("PENDING")
            listOf("attendances", "announcement_reads", "assignment_submissions", "after_party_invitees").forEach {
                assertThat(count(it, id)).isZero()
            }
        }
        assertThat(count("member_cohorts", 1)).isZero()
        assertThat(count("member_teams", 2)).isEqualTo(2)
        assertThat(jdbc.queryForObject("select role_id from member_roles where member_id = 2 and deleted_at is null", Long::class.java)).isEqualTo(2)
        // 실패 응답 뒤 같은 요청을 다시 보내면 정상 복구된다.
        service.approve(listOf(1, 2))
        assertInitialized(1)
        assertInitialized(2)
    }

    @Test
    fun `v1 v3 동시 승인은 같은 잠금을 기다리고 초기화를 한 번만 수행한다`() {
        val legacy =
            MemberController(
                queryService,
                mock(MemberCommandService::class.java),
                service,
                mock(MemberNameHashTypeValidator::class.java),
                mock(AppleAuthService::class.java),
                mock(EmailPasswordAuthService::class.java),
                mock(JwtTokenInjector::class.java),
                mock(DeviceIdResolver::class.java),
            )
        val current = MemberApprovalController(service)
        val changed = CountDownLatch(1)
        val release = CountDownLatch(1)
        val connectionId = AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            legacy.checkWhiteList(WhiteListCheckRequest(listOf(1, 2, 1)))
                            connectionId.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            changed.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(changed.await(10, TimeUnit.SECONDS))
            val second = pool.submit(Callable { current.approve(MemberApprovalRequest(listOf(2, 1))) })
            awaitDatabaseLockWait(connectionId.get())
            release.countDown()
            first.get(10, TimeUnit.SECONDS)
            second.get(10, TimeUnit.SECONDS)
            listOf(1L, 2L).forEach {
                assertInitialized(it)
                assertThat(count("member_cohorts", it)).isEqualTo(if (it == 2L) 2 else 1)
                assertThat(jdbc.queryForObject("select count(*) from member_roles where member_id = ? and cohort_id = 19 and deleted_at is null", Int::class.java, it)).isEqualTo(1)
            }
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `서로 다른 무소속 대기자를 동시에 승인할 수 있다`() {
        listOf(7L, 8L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at, updated_at) values (?, 'other', ?, 'WEB', 'PENDING', now(6), now(6))", id, "signup$id@example.com")
            jdbc.update("insert into member_roles (member_id, role_id, granted_at) values (?, 3, now(6))", id)
        }
        val ready = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures =
                listOf(7L, 8L).map { id ->
                    pool.submit(
                        Callable {
                            ready.countDown()
                            check(ready.await(10, TimeUnit.SECONDS))
                            service.approve(listOf(id))
                        },
                    )
                }
            futures.forEach { it.get(20, TimeUnit.SECONDS) }
            assertInitialized(7)
            assertInitialized(8)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun assertInitialized(memberId: Long) {
        listOf("attendances", "announcement_reads", "assignment_submissions", "after_party_invitees").forEach {
            assertThat(count(it, memberId)).describedAs("$it memberId=$memberId").isEqualTo(1)
        }
    }

    private fun count(
        table: String,
        memberId: Long,
    ): Int = jdbc.queryForObject("select count(*) from $table where member_id = ?", Int::class.java, memberId)!!

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
        error("두 번째 승인이 첫 트랜잭션의 잠금을 기다리지 않았습니다")
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
