package core.it.member

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.member.application.service.MemberManagementQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.request.MemberManagementRequest
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.cohort.MemberCohortRepository
import core.persistence.member.repository.role.MemberRoleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MemberRepository::class, MemberRoleRepository::class, MemberCohortRepository::class, CurrentCohortRoleResolver::class, MemberManagementQueryService::class, AttendanceGraduationEvaluator::class)
class MemberManagementMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var cohorts: CohortPersistencePort

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var roles: MemberRolePersistencePort

    @Autowired lateinit var sessions: SessionPersistencePort

    @Autowired lateinit var attendances: AttendancePersistencePort

    @Autowired lateinit var service: MemberManagementQueryService

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @Test
    fun `현재 기수의 단일 회원과 팀 역할을 읽고 출석집계 및 기본정보 시각을 보존한다`() {
        val old = cohorts.save(Cohort(value = "18")).id!!
        val current = cohorts.save(Cohort(value = "19")).id!!
        // 번호가 더 큰 준비 기수가 있어도 활성 기수를 조회해야 한다.
        cohorts.save(Cohort(value = "20"))
        jdbc.update("update cohorts set is_active = (cohort_id = ?)", current.value)
        val at = Instant.parse("2026-10-06T02:03:04Z")
        (1L..8L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at) values (?, ?, ?, 'SERVER', 'ACTIVE', '2026-10-01 00:00:00')", id, "member$id", "member$id@example.com")
        }
        // JPA Instant 쓰기 -> jOOQ LocalDateTime 읽기에서 시각이 9시간 이동하지 않아야 한다.
        members.save(Member(id = MemberId(1), name = "member1", signupEmail = "member1@example.com", part = MemberPart.SERVER, status = MemberStatus.ACTIVE, createdAt = at.minusSeconds(10), updatedAt = at))
        jdbc.update("update members set status = 'PENDING' where member_id in (3, 4)")
        jdbc.update("update members set deleted_at = '2026-10-02 00:00:00' where member_id = 5")
        jdbc.update("update members set status = 'WITHDRAWN' where member_id = 6")
        listOf(1L, 2L, 5L, 6L, 7L, 8L).forEach { joinCohort(it, current.value) }
        joinCohort(1, old.value)
        joinCohort(1, current.value) // 중복 소속 행은 인원 수를 늘리지 않는다.
        joinCohort(4, old.value) // 이전 기수만 있는 PENDING은 이번 관리 대상이 아니다.
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER'), (2, 'CORE'), (3, 'ORGANIZER'), (4, 'MASTER')")
        addRole(1, 1, current.value)
        addRole(1, 3, old.value)
        addRole(2, 2, current.value)
        addRole(2, 4, null)
        addRole(3, 2, current.value) // 현재 기수 역할만 있고 소속이 없으면 총원에서 제외.
        addRole(7, 1, current.value)
        addRole(8, 3, current.value, deleted = true)
        team(1, 101, 1, old.value)
        team(1, 102, 2, current.value)
        team(1, 103, 3, current.value)
        team(2, 104, 4, current.value)
        val start = Instant.parse("2026-09-01T03:00:00Z")
        val first = sessions.save(Session(cohortId = current, date = start, week = 1, place = "test", eventName = "test", isOnline = true, attendancePolicy = AttendancePolicy(start, start.plusSeconds(600), start.plusSeconds(1200), "1234")))
        val deleted = sessions.save(Session(cohortId = current, date = start, week = 2, place = "test", eventName = "test", isOnline = false, deletedAt = start, attendancePolicy = AttendancePolicy(start, start.plusSeconds(600), start.plusSeconds(1200), "1234")))
        attendances.save(Attendance(sessionId = first.id!!, memberId = MemberId(1), status = AttendanceStatus.ABSENT))
        attendances.save(Attendance(sessionId = deleted.id!!, memberId = MemberId(1), status = AttendanceStatus.ABSENT))
        attendances.save(Attendance(sessionId = first.id!!, memberId = MemberId(7), status = AttendanceStatus.ABSENT, deletedAt = start))

        val rows = members.findManagementMembers(current.value)
        assertThat(rows.map { it.memberId }).containsExactlyInAnyOrder(1L, 2L, 3L, 7L, 8L)
        assertThat(rows.single { it.memberId == 1L }.teamNumber).isEqualTo(3)
        assertThat(rows.single { it.memberId == 1L }.updatedAt).isEqualTo(at)
        assertThat(rows.single { it.memberId == 3L }.cohortId).isNull()
        assertThat(roles.findActiveRoleAssignmentsByMemberIds(listOf(1, 2, 8))[8]).isNull()
        assertThat(roles.findActiveRoleAssignmentsByMemberIds(emptyList())).isEmpty()
        val summary = attendances.findSummariesByCohortAndMemberIds(current.value, listOf(1, 7))
        assertThat(summary.keys).containsExactly(1L)
        assertThat(summary[1]!!.totalSessionCount).isEqualTo(1)
        assertThat(summary[1]!!.onlineAbsentCount).isEqualTo(1)
        assertThat(summary[1]!!.offlineAbsentCount).isZero()
        assertThat(attendances.findSummariesByCohortAndMemberIds(current.value, emptyList())).isEmpty()

        val response = service.getOverview(MemberManagementRequest(teamNumber = 3))
        assertThat(response.cohortId).isEqualTo(current.value)
        assertThat(response.members.map { it.memberId }).containsExactly(1L)
        assertThat(response.totalElements).isEqualTo(1)
        assertThat(response.summary.totalMemberCount).isEqualTo(3)
        assertThat(response.summary.pendingCount).isEqualTo(1)
        assertThat(response.summary.graduationRiskCount).isEqualTo(1)
        assertThat(response.summary.missingInformationCount).isEqualTo(2)
    }

    private fun joinCohort(
        memberId: Long,
        cohortId: Long,
    ) {
        jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, ?)", memberId, cohortId)
    }

    private fun addRole(
        memberId: Long,
        roleId: Long,
        cohortId: Long?,
        deleted: Boolean = false,
    ) {
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at, deleted_at) values (?, ?, ?, now(6), ?)", memberId, roleId, cohortId, if (deleted) "2026-10-01 00:00:00" else null)
    }

    private fun team(
        memberId: Long,
        teamId: Long,
        number: Int,
        cohortId: Long,
    ) {
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (?, ?, ?, 0, 0)", teamId, number, cohortId)
        jdbc.update("insert into member_teams (member_id, team_id) values (?, ?)", memberId, teamId)
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
