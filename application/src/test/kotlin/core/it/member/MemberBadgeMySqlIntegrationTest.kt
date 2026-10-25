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
import core.application.member.application.service.MemberProfileService
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
import core.persistence.member.repository.MemberAccountRepository
import core.persistence.member.repository.MemberAdmissionEventRepository
import core.persistence.member.repository.MemberBadgeRepository
import core.persistence.member.repository.MemberMergeRepository
import core.persistence.member.repository.MemberProfileRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import core.persistence.refreshToken.repository.RefreshTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
    core.application.cohort.application.service.CohortCommandService::class,
    core.application.cohort.application.service.CohortRoleService::class,
    core.application.member.application.service.access.MemberAccessService::class,
    core.application.member.application.service.MemberActivityService::class, core.persistence.member.repository.MemberActivityRepository::class,
    MemberBadgeTrackingConfiguration::class, MemberBadgeTransactionTracker::class, MemberBadgeService::class,
    MemberManagementTargetQueryService::class, AttendanceGraduationEvaluator::class, CurrentCohortRoleResolver::class,
    MemberBadgeRepository::class, MemberRepository::class, MemberRoleRepository::class, MemberTeamRepository::class,
    MemberCohortRepository::class, core.application.member.application.service.cohort.MemberCohortService::class, MemberAdmissionEventRepository::class, MemberAdmissionService::class,
    MemberMergeRepository::class, MemberAccountRepository::class, RefreshTokenRepository::class,
    MemberMergeService::class,
    MemberProfileService::class, MemberProfileRepository::class,
    MemberDeletionService::class, MemberApprovalService::class, MemberManagementCommandService::class, BadgeFixtureWriter::class,
)
class MemberBadgeMySqlIntegrationTest {
    @Autowired lateinit var cohortCommands: core.application.cohort.application.service.CohortCommandService

    @Autowired lateinit var cohortQueries: core.application.cohort.application.service.CohortQueryService

    @Autowired lateinit var access: core.application.member.application.service.access.MemberAccessService

    @Autowired lateinit var resolver: CurrentCohortRoleResolver

    @Autowired lateinit var memberRolePort: core.domain.member.port.outbound.MemberRolePersistencePort

    @Autowired lateinit var dsl: org.jooq.DSLContext

    @Autowired lateinit var entityManager: jakarta.persistence.EntityManager

    @Autowired lateinit var managementTargets: MemberManagementTargetQueryService

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var badges: MemberBadgeService

    @Autowired lateinit var admission: MemberAdmissionService

    @Autowired lateinit var merge: MemberMergeService

    @Autowired lateinit var deletion: MemberDeletionService

    @Autowired lateinit var approvals: MemberApprovalService

    @Autowired lateinit var management: MemberManagementCommandService

    @Autowired lateinit var profiles: MemberProfileService

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
        core.it.member.MemberActivityFixtures.clear(jdbc)
        listOf("role_permissions", "permissions", "member_merge_events", "member_oauth_link_requests", "refresh_tokens", "member_badge_memberships", "member_badge_states", "member_admission_events", "attendances", "sessions", "member_roles", "member_teams", "member_cohorts", "member_oauth", "members", "roles", "teams", "cohorts").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        (1L..3L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at, updated_at) values (?, '홍길동', ?, 'SERVER', 'PENDING', now(6), now(6))", id, "test$id@example.com")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
        }
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER')")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (191, 1, 19, 0, 0)")
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
        `when`(memberQueries.getMemberIdsByCohortId(CohortId(19))).thenReturn(listOf(MemberId(1), MemberId(2), MemberId(3)))
        core.it.member.MemberActivityFixtures.seed(jdbc)
    }

    @Test
    fun `중복 의심 회원도 개별 승인과 반려가 가능하다`() {
        assertThat(managementTargets.findAll(19).filter { it.memberId in listOf(1L, 2L) }).hasSize(2).allMatch { it.duplicateSuspected }
        approvals.approve(listOf(1))
        admission.reject(2)
        assertThat(jdbc.queryForObject("select status from members where member_id = 1", String::class.java)).isEqualTo("ACTIVE")
        assertThat(jdbc.queryForObject("select status from members where member_id = 2", String::class.java)).isEqualTo("REJECTED")
        assertThat(jdbc.queryForObject("select count(*) from member_merge_events", Int::class.java)).isZero()
    }

    @Test
    fun `첫 쓰기 신규 진입과 반려 재신청은 조회가 없어도 각각 보존된다`() {
        writer.signup("new@example.com")
        assertThat(card().version).isEqualTo(1)
        badges.acknowledge(MemberBadgeCard.PENDING, 19, 1)
        admission.reject(1)
        profiles.complete(1, "홍길동", "SERVER")
        admission.reapply(1)
        assertThat(card().version).isEqualTo(2)
        assertThat(card().hasNew).isTrue()
        profiles.complete(1, "홍길동", "SERVER")
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

    @ParameterizedTest
    @CsvSource("true, ''", "false, UNASSIGNED")
    fun `배지 초기화 전 잘못된 파트의 대기 회원도 기수 유무와 무관하게 프로필을 완료한다`(
        currentCohort: Boolean,
        invalidPart: String,
    ) {
        jdbc.update("update members set name = '', part = ? where member_id = 1", invalidPart)
        if (!currentCohort) jdbc.update("delete from member_cohorts where member_id = 1")

        val response = profiles.complete(1, "김가상", "WEB")

        assertThat(response.name).isEqualTo("김가상")
        assertThat(response.part).isEqualTo("WEB")
        assertThat(response.profileCompletionRequired).isFalse()
        assertThat(jdbc.queryForObject("select name from members where member_id = 1", String::class.java)).isEqualTo("김가상")
        assertThat(jdbc.queryForObject("select part from members where member_id = 1", String::class.java)).isEqualTo("WEB")
        assertThat(jdbc.queryForObject("select profile_completed_at from members where member_id = 1", java.sql.Timestamp::class.java)).isNotNull()
        assertThat(jdbc.queryForObject("select count(*) from member_badge_states where initialized = true", Int::class.java)).isEqualTo(3)
        assertThat(jdbc.queryForList("select member_id from member_badge_memberships where card = 'PENDING' order by member_id", Long::class.java)).containsExactly(1L, 2L, 3L)
        assertThat(badges.getBadges().cards).allMatch { !it.hasNew && it.version == 0L }
    }

    @Test
    fun `배지 초기화 후 다른 승인 회원의 잘못된 파트가 있어도 프로필 완료와 배지 갱신을 커밋한다`() {
        jdbc.update("update members set status = 'ACTIVE' where member_id = 2")
        MemberActivityFixtures.seed(jdbc)
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (2, 1, 19, now(6))")
        jdbc.update("insert into member_teams (member_id, team_id) values (2, 191)")
        badges.getBadges()
        jdbc.update("update members set part = 'UNKNOWN' where member_id = 2")

        profiles.complete(1, "김가상", "WEB")

        assertThat(jdbc.queryForObject("select name from members where member_id = 1", String::class.java)).isEqualTo("김가상")
        assertThat(jdbc.queryForObject("select part from members where member_id = 1", String::class.java)).isEqualTo("WEB")
        assertThat(jdbc.queryForObject("select profile_completed_at from members where member_id = 1", java.sql.Timestamp::class.java)).isNotNull()
        assertThat(jdbc.queryForObject("select part from members where member_id = 2", String::class.java)).isEqualTo("UNKNOWN")
        assertThat(jdbc.queryForList("select member_id from member_badge_memberships where card = 'PENDING' order by member_id", Long::class.java)).containsExactly(1L, 3L)
        assertThat(jdbc.queryForList("select member_id from member_badge_memberships where card = 'INCOMPLETE'", Long::class.java)).containsExactly(2L)
        assertThat(card().version).isZero()
        assertThat(card().hasNew).isFalse()
        assertThat(card(MemberBadgeCard.INCOMPLETE).version).isEqualTo(1)
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isTrue()
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
        profiles.complete(1, "홍길동", "SERVER")
        admission.reapply(1)
        val oldVersion = card().version
        admission.reject(2)
        profiles.complete(2, "홍길동", "SERVER")
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
        MemberActivityFixtures.seed(jdbc)
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
        MemberActivityFixtures.seed(jdbc)
        badges.getBadges()
        val command = SessionCreateCommand(Instant.parse("2026-11-01T03:00:00Z"), 1, "온라인", "테스트 세션", true)
        sessions.createSession(command)
        val first = jdbc.queryForObject("select max(session_id) from sessions", Long::class.java)!!
        attendance.updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(first), MemberId(1), AttendanceStatus.ABSENT))
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(1)
        repeat(4) { sessions.createSession(command.copy(date = command.date.plusSeconds((it + 1) * 86400L), week = it + 2)) }
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
        val last = jdbc.queryForObject("select max(session_id) from sessions", Long::class.java)!!
        sessions.softDeleteSession(SessionId(last))
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(2)
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isTrue()
    }

    @Test
    fun `자동 마감과 늦게 처리된 마감 전 인증도 NEW 전이를 저장한다`() {
        jdbc.update("update members set status = 'ACTIVE'")
        MemberActivityFixtures.seed(jdbc)
        badges.getBadges()
        val start = Instant.parse("2026-11-01T03:00:00Z")
        repeat(10) {
            val sessionStart = start.plusSeconds(it * 86400L)
            sessions.createSession(SessionCreateCommand(sessionStart, it + 1, "온라인", "테스트 세션", true, sessionStart, sessionStart.plusSeconds(600), sessionStart.plusSeconds(1200)))
        }
        val ids = jdbc.queryForList("select session_id from sessions order by session_id limit 3", Long::class.java)
        ids.forEachIndexed { index, id ->
            attendance.closeExpiredAttendances(SessionId(id), start.plusSeconds(index * 86400L + 1201))
        }
        assertThat(card(MemberBadgeCard.AT_RISK).version).isEqualTo(3)
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isTrue()
        val code = jdbc.queryForObject("select attendance_code from sessions where session_id = ?", String::class.java, ids.last())!!
        (1L..3L).forEach { memberId ->
            attendance.attendSession(AttendanceRecordCommand(SessionId(ids.last()), MemberId(memberId), start.plusSeconds(2 * 86400L + 1), code))
        }
        assertThat(card(MemberBadgeCard.AT_RISK).hasNew).isFalse()
    }

    @Test
    fun `수동 SQL은 재실행 가능하고 Hibernate 엔티티 스키마 검증을 통과한다`() {
        val root =
            generateSequence(java.nio.file.Path.of("").toAbsolutePath()) { it.parent }
                .first { java.nio.file.Files.isDirectory(it.resolve("db")) }
        val sql =
            java.nio.file.Files.walk(root.resolve("db")).use { paths ->
                paths.filter { it.fileName.toString() == "2610091830_member_badges.sql" }
                    .findFirst().orElseThrow()
            }
        jdbc.execute("drop table member_badge_memberships")
        jdbc.execute("drop table member_badge_states")
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
        merge.mergeAndApprove(1, 2, 99)
        assertThat(card().hasNew).isFalse()
        assertThat(card(MemberBadgeCard.INCOMPLETE).hasNew).isTrue()
        assertThat(jdbc.queryForList("select member_id from member_badge_memberships where card = 'PENDING'", Long::class.java)).containsExactly(3L)
    }

    @Test
    fun `actual cohort rename and activation preserve loaded JPA memberships teams and suspension history`() {
        prepareCohortTransition()
        val beforeMemberships = jdbc.queryForList("select member_cohort_id, member_id, cohort_id from member_cohorts order by member_cohort_id")
        val beforeTeams = jdbc.queryForList("select team_id, cohort_id from teams order by team_id")
        val suspendedAt = jdbc.queryForMap("select suspended_at from member_cohort_activities where member_id=1 and cohort_id=19")["suspended_at"]
        rc().executeWithoutResult {
            assertLoadedActiveCohortWithAssociations()
            cohortCommands.update(CohortId(19), "19-renamed")
            val active = cohortCommands.activateCohort(CohortId(20))
            assertThat(active.isActive).isTrue()
            entityManager.flush()
        }
        assertThat(jdbc.queryForList("select member_cohort_id, member_id, cohort_id from member_cohorts order by member_cohort_id")).isEqualTo(beforeMemberships)
        assertThat(jdbc.queryForList("select team_id, cohort_id from teams order by team_id")).isEqualTo(beforeTeams)
        assertThat(jdbc.queryForMap("select status, suspended_at from member_cohort_activities where member_id=1 and cohort_id=19"))
            .containsEntry("status", "ENDED").containsEntry("suspended_at", suspendedAt)
        assertThat(jdbc.queryForObject("select count(*) from cohorts where is_active=true", Int::class.java)).isEqualTo(1)
        assertThat(cohortQueries.getActiveCohortId()).isEqualTo(CohortId(20))
        assertThat(jdbc.queryForObject("select count(*) from member_activity_events where member_id=1", Int::class.java)).isEqualTo(3)
    }

    @Test
    fun `rename waiting behind cohort activation cannot restore stale active flag or remove associations`() {
        prepareCohortTransition()
        val beforeMemberships = jdbc.queryForList("select member_cohort_id from member_cohorts order by member_cohort_id", Long::class.java)
        val beforeTeams = jdbc.queryForList("select team_id from teams order by team_id", Long::class.java)
        val switched = CountDownLatch(1)
        val release = CountDownLatch(1)
        val staleLoaded = CountDownLatch(1)
        val blockingConnection = java.util.concurrent.atomic.AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        rc().executeWithoutResult {
                            cohortCommands.activateCohort(CohortId(20))
                            blockingConnection.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            switched.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(switched.await(10, TimeUnit.SECONDS))
            val second =
                pool.submit(
                    Callable {
                        rc().executeWithoutResult {
                            // Reproduce the pre-lock persistence-context snapshot produced by badge tracking.
                            assertLoadedActiveCohortWithAssociations()
                            staleLoaded.countDown()
                            cohortCommands.update(CohortId(19), "19-renamed")
                        }
                    },
                )
            check(staleLoaded.await(10, TimeUnit.SECONDS))
            awaitCohortLockWait(blockingConnection.get())
            release.countDown()
            first.get(10, TimeUnit.SECONDS)
            second.get(10, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
        assertThat(jdbc.queryForList("select member_cohort_id from member_cohorts order by member_cohort_id", Long::class.java)).isEqualTo(beforeMemberships)
        assertThat(jdbc.queryForList("select team_id from teams order by team_id", Long::class.java)).isEqualTo(beforeTeams)
        assertThat(jdbc.queryForObject("select count(*) from cohorts where is_active=true", Int::class.java)).isEqualTo(1)
        assertThat(cohortQueries.getActiveCohortId()).isEqualTo(CohortId(20))
        assertThat(jdbc.queryForMap("select status, suspended_at from member_cohort_activities where member_id=1 and cohort_id=19"))
            .containsEntry("status", "ENDED").doesNotContainEntry("suspended_at", null)
        assertThat(jdbc.queryForObject("select count(*) from member_badge_states where cohort_id=20 and initialized=true", Int::class.java)).isEqualTo(3)
    }

    @Test
    fun `suspended cohort master keeps no permissions after rollover until explicitly approved into new cohort`() {
        jdbc.update("insert into roles (role_id, name) values (4, 'MASTER')")
        jdbc.update("insert into permissions (permission_id, resource, action) values (668, 'session', 'read')")
        jdbc.update("insert into role_permissions (role_id, permission_id, granted_at) values (4, 668, now(6))")
        jdbc.update("insert into members (member_id, name, signup_email, status, created_at, updated_at) values (4, 'global', 'global@example.com', 'ACTIVE', now(6), now(6))")
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (1, 4, null, now(6)), (4, 4, null, now(6))")
        approvals.approve(listOf(1))
        val roleRepository =
            core.persistence.authorization.repository.RoleRepository(
                org.mockito.Mockito.mock(core.persistence.authorization.repository.RoleJpaRepository::class.java),
                dsl,
            )
        val queries = core.application.authorization.application.service.RoleQueryService(roleRepository, memberRolePort, resolver, cohortQueries)
        val properties =
            core.application.security.properties.TokenProperties(
                java.util.Base64.getEncoder().encodeToString(ByteArray(64) { 7 }),
                core.application.security.properties.TokenProperties.ExpirationTime(600, 1200),
            )
        val tokens = core.application.security.oauth.token.JwtTokenProvider(properties, queries)
        val token = tokens.generateAccessToken("1", null)
        assertThat(tokens.getAuthentication(token).authorities.map { it.authority }).contains("read:session")
        management.update(1, MemberManagementUpdateRequest(activityStatus = core.domain.member.enums.MemberActivityStatus.SUSPENDED))
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (20, '20', false, 0, 0)")
        cohortCommands.activateCohort(CohortId(20))
        assertThat(tokens.getAuthentication(token).isAuthenticated).isTrue()
        assertThat(tokens.getAuthentication(token).authorities).isEmpty()
        assertThat(access.isAdmin(MemberId(1))).isFalse()
        assertThat(access.isAdmin(MemberId(4))).isTrue()
        assertThat(access.getIsAdminByMemberIds(listOf(MemberId(1), MemberId(4))))
            .containsEntry(MemberId(1), false).containsEntry(MemberId(4), true)
        approvals.approve(listOf(1))
        assertThat(access.isAdmin(MemberId(1))).isTrue()
        assertThat(access.getIsAdminByMemberIds(listOf(MemberId(1), MemberId(4))).values).containsOnly(true)
        assertThat(tokens.getAuthentication(token).authorities.map { it.authority }).contains("read:session")
        assertThat(jdbc.queryForMap("select status, suspended_at from member_cohort_activities where member_id=1 and cohort_id=19"))
            .containsEntry("status", "ENDED").doesNotContainEntry("suspended_at", null)
    }

    private fun prepareCohortTransition() {
        approvals.approve(listOf(1))
        management.update(1, MemberManagementUpdateRequest(activityStatus = core.domain.member.enums.MemberActivityStatus.SUSPENDED))
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (20, '20', false, 0, 0)")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (201, 1, 20, 0, 0)")
    }

    private fun assertLoadedActiveCohortWithAssociations() {
        // entity is a runtime-only module of application; keep the test at the existing module boundary.
        val type = Class.forName("core.entity.cohort.CohortEntity")
        val loaded = entityManager.find(type, 19L)
        assertThat(type.getMethod("isActive").invoke(loaded)).isEqualTo(true)
        assertThat(type.getMethod("getMemberCohorts").invoke(loaded) as Collection<*>).hasSize(3)
        assertThat(type.getMethod("getTeams").invoke(loaded) as Collection<*>).hasSize(1)
    }

    private fun awaitCohortLockWait(connectionId: Long) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val waits = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w join performance_schema.threads t on t.thread_id=w.blocking_thread_id where t.processlist_id=?", Long::class.java, connectionId)!!
            if (waits > 0) return
            CountDownLatch(1).await(20, TimeUnit.MILLISECONDS)
        }
        error("Cohort rename did not wait for activation lock")
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
