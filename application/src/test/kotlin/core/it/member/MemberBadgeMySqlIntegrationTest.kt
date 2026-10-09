package core.it.member

import core.application.attendance.application.service.AttendanceCommandService
import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberAdmissionService
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberBadgeService
import core.application.member.application.service.MemberBadgeTrackingConfiguration
import core.application.member.application.service.MemberBadgeTransactionTracker
import core.application.member.application.service.MemberDeletionService
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.MemberManagementTargetQueryService
import core.application.member.application.service.MemberMergeService
import core.application.member.application.service.TrackMemberBadges
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.application.member.presentation.response.MemberBadgeCard
import core.application.session.application.service.SessionCommandService
import core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.vo.SessionId
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.member.repository.MemberAdmissionEventRepository
import core.persistence.member.repository.MemberBadgeRepository
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(
    MemberBadgeTrackingConfiguration::class, MemberBadgeTransactionTracker::class, MemberBadgeService::class,
    MemberManagementTargetQueryService::class, AttendanceGraduationEvaluator::class, CurrentCohortRoleResolver::class,
    MemberBadgeRepository::class, MemberRepository::class, MemberRoleRepository::class, MemberTeamRepository::class,
    MemberCohortRepository::class, core.application.member.application.service.cohort.MemberCohortService::class, MemberAdmissionEventRepository::class, MemberAdmissionService::class,
    MemberMergeRepository::class,
    MemberMergeService::class,
    MemberDeletionService::class, MemberApprovalService::class, MemberManagementCommandService::class, BadgeFixtureWriter::class,
)
class MemberBadgeMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var badges: MemberBadgeService

    @Autowired lateinit var admission: MemberAdmissionService

    @Autowired lateinit var merge: MemberMergeService

    @Autowired lateinit var deletion: MemberDeletionService

    @Autowired lateinit var approvals: MemberApprovalService

    @Autowired lateinit var management: MemberManagementCommandService

    @Autowired lateinit var attendance: AttendanceCommandService

    @Autowired lateinit var sessions: SessionCommandService

    @Autowired lateinit var writer: BadgeFixtureWriter

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var memberQueries: MemberQueryUseCase

    @MockitoBean lateinit var roleQueries: RoleQueryUseCase

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @MockitoBean lateinit var feedbackForms: SessionFeedbackFormCommandService

    @MockitoBean lateinit var initializer: MemberActivationInitializer

    @BeforeEach
    fun fixture() {
        listOf("member_badge_memberships", "member_badge_states", "member_admission_events", "attendances", "sessions", "member_roles", "member_teams", "member_cohorts", "member_oauth", "members", "roles", "teams", "cohorts").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        (1L..3L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at, updated_at) values (?, '홍길동', ?, 'SERVER', 'PENDING', now(6), now(6))", id, "test$id@example.com")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
        }
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER')")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (191, 1, 19, 0, 0)")
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
        `when`(memberQueries.getMemberIdsByCohortId(CohortId(19))).thenReturn(listOf(MemberId(1), MemberId(2), MemberId(3)))
    }

    @Test
    fun `첫 쓰기 신규 진입과 반려 재신청은 조회가 없어도 각각 보존된다`() {
        writer.signup("new@example.com")
        assertThat(card().version).isEqualTo(1)
        badges.acknowledge(MemberBadgeCard.PENDING, 19, 1)
        admission.reject(1)
        admission.reapply(1)
        assertThat(card().version).isEqualTo(2)
        assertThat(card().hasNew).isTrue()
        admission.reapply(1)
        assertThat(card().version).isEqualTo(2)
    }

    @Test
    fun `승인 정보 완성 재진입 삭제는 카드 전이를 같은 커밋에 저장한다`() {
        badges.getBadges()
        approvals.approve(listOf(1))
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isTrue()
        management.update(1, MemberManagementUpdateRequest(teamId = 191))
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isFalse()
        management.update(1, MemberManagementUpdateRequest(part = "UNASSIGNED"))
        assertThat(card(MemberBadgeCard.INCOMPLETE).version).isEqualTo(2)
        deletion.delete(listOf(1), 3)
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isFalse()
    }

    @Test
    fun `동시 회원 변경은 커밋 전 최신 snapshot으로 합쳐져 진입을 잃지 않는다`() {
        jdbc.update("update members set status = 'REJECTED'")
        badges.getBadges()
        val firstWritten = CountDownLatch(1)
        val secondCommitted = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        rc().executeWithoutResult {
                            writer.makePending(1)
                            firstWritten.countDown()
                            check(secondCommitted.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            val second =
                pool.submit(
                    Callable {
                        check(firstWritten.await(10, TimeUnit.SECONDS))
                        writer.makePending(2)
                        secondCommitted.countDown()
                    },
                )
            first.get(15, TimeUnit.SECONDS)
            second.get(15, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
        }
        assertThat(card().version).isEqualTo(2)
        assertThat(jdbc.queryForObject("select count(*) from member_badge_memberships where card = 'PENDING'", Int::class.java)).isEqualTo(2)
    }

    @Test
    fun `확인 중 새 진입이 기다려도 이전 버전 확인이 새 NEW를 지우지 않는다`() {
        admission.reject(1)
        admission.reapply(1)
        val oldVersion = card().version
        admission.reject(2)
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val ack =
                pool.submit(
                    Callable {
                        rc().executeWithoutResult {
                            badges.acknowledge(MemberBadgeCard.PENDING, 19, oldVersion)
                            locked.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(locked.await(10, TimeUnit.SECONDS))
            val entry = pool.submit(Callable { admission.reapply(2) })
            release.countDown()
            ack.get(15, TimeUnit.SECONDS)
            entry.get(15, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
        }
        assertThat(card().version).isEqualTo(oldVersion + 1)
        assertThat(badges.acknowledge(MemberBadgeCard.PENDING, 19, oldVersion).hasNew).isTrue()
    }

    @Test
    fun `배지 beforeCommit 실패는 회원 상태와 반려 이력도 롤백한다`() {
        badges.getBadges()
        jdbc.execute("create trigger member624_fail_badge before update on member_badge_states for each row signal sqlstate '45000' set message_text = 'badge rollback test'")
        try {
            assertThatThrownBy { admission.reject(1) }.isInstanceOf(RuntimeException::class.java)
            assertThat(jdbc.queryForObject("select status from members where member_id = 1", String::class.java)).isEqualTo("PENDING")
            assertThat(jdbc.queryForObject("select count(*) from member_admission_events", Int::class.java)).isZero()
        } finally {
            jdbc.execute("drop trigger member624_fail_badge")
        }
    }

    @Test
    fun `외부 RR 트랜잭션은 변경 전에 차단하여 오래된 snapshot 저장을 방지한다`() {
        assertThatThrownBy {
            TransactionTemplate(transactionManager).apply { isolationLevel = TransactionDefinition.ISOLATION_REPEATABLE_READ }.executeWithoutResult { admission.reject(1) }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(jdbc.queryForObject("select status from members where member_id = 1", String::class.java)).isEqualTo("PENDING")
    }

    @Test
    fun `세션 생성 출석 생성과 출석 변경 세션 삭제가 수료 카드에 함께 반영된다`() {
        jdbc.update("update members set status = 'ACTIVE'")
        badges.getBadges()
        sessions.createSession(SessionCreateCommand(Instant.parse("2026-11-01T03:00:00Z"), 1, "온라인", "테스트 세션", true))
        val id = jdbc.queryForObject("select max(session_id) from sessions", Long::class.java)!!
        assertThat(jdbc.queryForObject("select count(*) from attendances where session_id = ?", Int::class.java, id)).isEqualTo(3)
        attendance.updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(id), MemberId(1), AttendanceStatus.ABSENT))
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isTrue()
        attendance.updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(id), MemberId(1), AttendanceStatus.PRESENT))
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
        attendance.updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(id), MemberId(1), AttendanceStatus.ABSENT))
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(2)
        sessions.softDeleteSession(SessionId(id))
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
    }

    @Test
    fun `세션 분모 변경은 위험 이탈과 재진입을 새 버전으로 저장한다`() {
        jdbc.update("update members set status = 'ACTIVE'")
        badges.getBadges()
        val command = SessionCreateCommand(Instant.parse("2026-11-01T03:00:00Z"), 1, "온라인", "테스트 세션", true)
        sessions.createSession(command)
        val first = jdbc.queryForObject("select max(session_id) from sessions", Long::class.java)!!
        attendance.updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(first), MemberId(1), AttendanceStatus.ABSENT))
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(1)
        repeat(4) { sessions.createSession(command.copy(week = it + 2)) }
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
        val last = jdbc.queryForObject("select max(session_id) from sessions", Long::class.java)!!
        sessions.softDeleteSession(SessionId(last))
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(2)
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isTrue()
    }

    @Test
    fun `자동 마감과 늦게 처리된 마감 전 인증도 NEW 전이를 저장한다`() {
        jdbc.update("update members set status = 'ACTIVE'")
        badges.getBadges()
        val start = Instant.parse("2026-11-01T03:00:00Z")
        repeat(10) { sessions.createSession(SessionCreateCommand(start, it + 1, "온라인", "테스트 세션", true, start, start.plusSeconds(600), start.plusSeconds(1200))) }
        val ids = jdbc.queryForList("select session_id from sessions order by session_id limit 3", Long::class.java)
        ids.forEach { attendance.closeExpiredAttendances(SessionId(it), start.plusSeconds(1201)) }
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(3)
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isTrue()
        val code = jdbc.queryForObject("select attendance_code from sessions where session_id = ?", String::class.java, ids.last())!!
        (1L..3L).forEach { memberId ->
            attendance.attendSession(AttendanceRecordCommand(SessionId(ids.last()), MemberId(memberId), start.plusSeconds(1), code))
        }
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
    }

    @Test
    fun `수동 SQL은 재실행 가능하고 Hibernate 엔티티 스키마 검증을 통과한다`() {
        jdbc.execute("drop table member_badge_memberships")
        jdbc.execute("drop table member_badge_states")
        val directory = listOf(java.nio.file.Path.of("db/pending"), java.nio.file.Path.of("../db/pending")).first { java.nio.file.Files.isDirectory(it) }
        val sql = java.nio.file.Files.list(directory).use { paths -> paths.filter { it.fileName.toString().endsWith("_member_badges.sql") }.findFirst().orElseThrow() }
        repeat(2) {
            jdbc.dataSource!!.connection.use { connection ->
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection, org.springframework.core.io.FileSystemResource(sql))
            }
        }
        val factory =
            org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean().apply {
                dataSource = jdbc.dataSource
                jpaVendorAdapter = org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter()
                setManagedTypes(org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes.of("core.entity.member.MemberBadgeStateEntity", "core.entity.member.MemberBadgeMembershipEntity"))
                setJpaPropertyMap(mapOf("hibernate.hbm2ddl.auto" to "validate"))
            }
        try {
            factory.afterPropertiesSet()
        } finally {
            factory.destroy()
        }
        assertThat(card().version).isZero()
    }

    @Test
    fun `기수가 없는 최초 쓰기도 rollback only가 되지 않고 확인 완료 seed를 만든다`() {
        jdbc.execute("delete from member_cohorts")
        jdbc.execute("delete from teams")
        jdbc.execute("delete from cohorts")
        writer.createCohort()
        assertThat(badges.getBadges().cards).allMatch { !it.hasNew && it.version == 0L }
    }

    @Test
    fun `회원 통합은 PENDING 이탈과 유지 회원의 정보 미입력 진입을 함께 저장한다`() {
        (1L..2L).forEach { id ->
            jdbc.update("insert into member_oauth (member_id, provider, external_id) values (?, ?, ?)", id, if (id == 1L) "KAKAO" else "APPLE", "external-$id")
        }
        badges.getBadges()
        merge.mergeAndApprove(1, 2)
        assertThat(card().hasNew).isFalse()
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isTrue()
        assertThat(jdbc.queryForList("select member_id from member_badge_memberships where card = 'PENDING'", Long::class.java)).containsExactly(3L)
    }

    private fun card(card: MemberBadgeCard = MemberBadgeCard.PENDING) = badges.getBadges().cards.single { it.card == card }

    private fun rc() = TransactionTemplate(transactionManager).apply { isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}

/** A provider-free writer isolates transaction/flush behavior from external authentication. */
@Service
class BadgeFixtureWriter(private val members: MemberPersistencePort, private val cohorts: core.domain.cohort.port.outbound.CohortPersistencePort) {
    @TrackMemberBadges
    fun signup(email: String) {
        members.save(Member.createPending(email, "홍길동"))
    }

    @TrackMemberBadges
    fun makePending(memberId: Long) {
        members.updateManagementFields(listOf(memberId), false, null, core.domain.member.enums.MemberStatus.PENDING, emptySet())
    }

    @TrackMemberBadges
    fun createCohort() {
        cohorts.save(core.domain.cohort.aggregate.Cohort(value = "20"))
    }
}
