package core.it.attendance

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.team.vo.TeamNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/** 수료 판정용 출석 집계 조회를 MySQL 에서 검증한다. 실행 조건은 [AttendanceConcurrencyMySqlIntegrationTest] 와 같다. */
@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(
    classes = [AttendanceMySqlIntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
class AttendanceOverviewMySqlIntegrationTest {
    @Autowired lateinit var cohortPort: CohortPersistencePort

    @Autowired lateinit var sessionPort: SessionPersistencePort

    @Autowired lateinit var attendancePort: AttendancePersistencePort

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean lateinit var sentSessionNotificationCommandUseCase: SentSessionNotificationCommandUseCase

    private val firstSessionStart = Instant.parse("2026-09-05T05:00:00Z")

    /** 다른 테스트의 고정 멤버 ID 와 겹치지 않도록 멤버/팀 ID 를 크게 잡는다. */
    private fun uniqueId(): Long = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L)

    @Test
    fun `사람별 상세는 최신 기수만, 팀 중복 없이, 삭제된 세션을 빼고 집계하며 결석 사유서를 붙인다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val memberId = newMember(name)
        // 두 기수 모두에 팀이 있고 현재 기수에는 팀이 두 번 배정됐다.
        joinTeam(memberId, oldCohort, teamNumber = 3)
        joinTeam(memberId, currentCohort, teamNumber = 5)
        joinTeam(memberId, currentCohort, teamNumber = 7)

        addAttendance(newSession(oldCohort, week = 1, isOnline = false), memberId, AttendanceStatus.ABSENT)

        val present = newSession(currentCohort, week = 1, isOnline = false)
        val late = newSession(currentCohort, week = 2, isOnline = true)
        val absent = newSession(currentCohort, week = 3, isOnline = false)
        val upcoming = newSession(currentCohort, week = 4, isOnline = false)
        // 합류 전에 마감돼 출석 기록이 없는 세션: 분모에는 들어가지만 결석이 아니다
        newSession(currentCohort, week = 5, isOnline = false)
        val deleted = newSession(currentCohort, week = 6, isOnline = false, deletedAt = firstSessionStart)
        addAttendance(present, memberId, AttendanceStatus.PRESENT)
        addAttendance(late, memberId, AttendanceStatus.LATE)
        addAttendance(absent, memberId, AttendanceStatus.ABSENT)
        addAttendance(upcoming, memberId, AttendanceStatus.PENDING)
        addAttendance(deleted, memberId, AttendanceStatus.ABSENT)
        addAbsenceReason(absent, memberId, "예전 사유", "REJECTED")
        addAbsenceReason(absent, memberId, "병원 진료", "PENDING")

        val expectedSummary =
            AttendanceSummaryQueryModel(
                totalSessionCount = 5,
                presentCount = 1,
                lateCount = 1,
                excusedAbsentCount = 0,
                onlineAbsentCount = 0,
                offlineAbsentCount = 1,
            )

        val detail = attendancePort.findDetailMemberAttendance(GetDetailMemberAttendancesQuery(MemberId(memberId))).single()
        // 표시 팀은 그 기수의 가장 최근 배정
        assertThat(detail.teamNumber).isEqualTo(TeamNumber(7))
        assertThat(detail.summary).isEqualTo(expectedSummary)

        val sessions = attendancePort.findMemberSessionAttendances(GetDetailMemberAttendancesQuery(MemberId(memberId)))
        assertThat(sessions.map { it.sessionId }).containsExactly(present.id!!.value, late.id!!.value, absent.id!!.value, upcoming.id!!.value)
        assertThat(sessions.map { it.sessionIsOnline }).containsExactly(false, true, false, false)
        assertThat(sessions.map { it.absenceReason?.contents }).containsExactly(null, null, "병원 진료", null)
        assertThat(sessions.single { it.sessionId == absent.id!!.value }.absenceReason)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(MemberSessionAttendanceQueryModel.AbsenceReason(id = 0, contents = "병원 진료", status = "PENDING"))

        // 같은 세션별 개인 상세의 수료 판정도 같은 집계를 쓴다
        val sessionDetail =
            attendancePort.findDetailAttendanceBySession(GetDetailAttendanceBySessionQuery(late.id!!, MemberId(memberId)))!!
        assertThat(sessionDetail.summary).isEqualTo(expectedSummary)
        assertThat(sessionDetail.teamNumber).isEqualTo(TeamNumber(7))

        // 출석 기록이 없는 멤버는 결과가 없다(서비스는 기존처럼 404)
        val newcomer = newMember(name)
        assertThat(attendancePort.findDetailMemberAttendance(GetDetailMemberAttendancesQuery(MemberId(newcomer)))).isEmpty()
        assertThat(attendancePort.findMemberSessionAttendances(GetDetailMemberAttendancesQuery(MemberId(newcomer)))).isEmpty()
    }

    @Test
    fun `사람별 목록은 기수별 한 행이고 상태와 팀 필터는 멤버만 고르며 집계는 전체 기록으로 한다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val absentMember = newMember(name)
        val presentMember = newMember(name)
        val noTeamMember = newMember(name)
        joinTeam(absentMember, oldCohort, teamNumber = 1)
        // 같은 기수 팀 매핑 중복
        joinTeam(absentMember, currentCohort, teamNumber = 2)
        joinTeam(absentMember, currentCohort, teamNumber = 2)
        joinTeam(presentMember, currentCohort, teamNumber = 2)

        addAttendance(newSession(oldCohort, week = 1, isOnline = true), absentMember, AttendanceStatus.PRESENT)
        val first = newSession(currentCohort, week = 1, isOnline = false)
        val second = newSession(currentCohort, week = 2, isOnline = true)
        addAttendance(first, absentMember, AttendanceStatus.ABSENT)
        addAttendance(second, absentMember, AttendanceStatus.LATE)
        addAttendance(first, presentMember, AttendanceStatus.PRESENT)
        addAttendance(second, presentMember, AttendanceStatus.PRESENT)
        addAttendance(first, noTeamMember, AttendanceStatus.ABSENT)

        val query =
            GetMemberAttendancesQuery(
                memberId = MemberId(absentMember),
                statuses = listOf(AttendanceStatus.ABSENT),
                teams = null,
                name = name,
                onlyMyTeam = null,
                page = 1,
                size = 20,
            )

        val rows = attendancePort.findMemberAttendancesByQuery(query, TeamNumber.defaultValue())

        // ABSENT 가 있는 기수의 행만, 지각까지 포함해 한 번씩 집계된다. 팀이 없는 멤버는 팀 0 으로 남는다(팀 번호 순)
        assertThat(rows.map { it.id }).containsExactly(noTeamMember, absentMember)
        assertThat(rows.map { it.teamNumber }).containsExactly(TeamNumber(0), TeamNumber(2))
        assertThat(rows.single { it.id == absentMember }.summary)
            .isEqualTo(
                AttendanceSummaryQueryModel(
                    totalSessionCount = 2,
                    presentCount = 0,
                    lateCount = 1,
                    excusedAbsentCount = 0,
                    onlineAbsentCount = 0,
                    offlineAbsentCount = 1,
                ),
            )
        assertThat(attendancePort.countMemberAttendancesByQuery(query, TeamNumber.defaultValue())).isEqualTo(2)

        val everyone = query.copy(statuses = null)
        assertThat(attendancePort.findMemberAttendancesByQuery(everyone, TeamNumber.defaultValue()).map { it.id })
            .containsExactlyInAnyOrder(absentMember, absentMember, presentMember, noTeamMember)
        assertThat(attendancePort.countMemberAttendancesByQuery(everyone, TeamNumber.defaultValue())).isEqualTo(4)

        val team2 = query.copy(statuses = null, teams = listOf(2))
        val team2Rows = attendancePort.findMemberAttendancesByQuery(team2, TeamNumber.defaultValue())
        assertThat(team2Rows.map { it.id }).containsExactlyInAnyOrder(absentMember, presentMember)
        assertThat(team2Rows.single { it.id == absentMember }.summary.lateCount).isEqualTo(1)
        assertThat(attendancePort.countMemberAttendancesByQuery(team2, TeamNumber.defaultValue())).isEqualTo(2)
    }

    @Test
    fun `사람별 목록의 팀 필터와 내 팀 필터는 표시하는 최신 팀 기준이고 같은 기수의 이전 팀으로는 걸리지 않는다`() {
        val name = uniqueName()
        val (_, currentCohort) = newCohortPair()
        val movedMember = newMember(name)
        joinTeam(movedMember, currentCohort, teamNumber = 5)
        joinTeam(movedMember, currentCohort, teamNumber = 7)
        addAttendance(newSession(currentCohort, week = 1, isOnline = false), movedMember, AttendanceStatus.PRESENT)

        val query =
            GetMemberAttendancesQuery(
                memberId = MemberId(movedMember),
                statuses = null,
                teams = null,
                name = name,
                onlyMyTeam = null,
                page = 1,
                size = 20,
            )
        val previousTeam = query.copy(teams = listOf(5))
        val latestTeam = query.copy(teams = listOf(7))
        val onlyMyTeam = query.copy(onlyMyTeam = true)

        assertThat(attendancePort.findMemberAttendancesByQuery(previousTeam, TeamNumber.defaultValue())).isEmpty()
        assertThat(attendancePort.countMemberAttendancesByQuery(previousTeam, TeamNumber.defaultValue())).isEqualTo(0)
        assertThat(attendancePort.findMemberAttendancesByQuery(latestTeam, TeamNumber.defaultValue()).map { it.teamNumber })
            .containsExactly(TeamNumber(7))
        assertThat(attendancePort.countMemberAttendancesByQuery(latestTeam, TeamNumber.defaultValue())).isEqualTo(1)
        assertThat(attendancePort.findMemberAttendancesByQuery(onlyMyTeam, TeamNumber(5))).isEmpty()
        assertThat(attendancePort.countMemberAttendancesByQuery(onlyMyTeam, TeamNumber(5))).isEqualTo(0)
        assertThat(attendancePort.findMemberAttendancesByQuery(onlyMyTeam, TeamNumber(7)).map { it.id })
            .containsExactly(movedMember)
    }

    /** 최신 기수 선택(기수 값의 숫자 크기)을 확인할 수 있도록 숫자 값의 이전/현재 기수를 만든다. */
    private fun newCohortPair(): Pair<CohortId, CohortId> {
        val base = ThreadLocalRandom.current().nextLong(100_000_000L, 900_000_000L)
        return cohortPort.save(Cohort(value = base.toString())).id!! to cohortPort.save(Cohort(value = (base + 1).toString())).id!!
    }

    private fun newMember(name: String): Long {
        val memberId = uniqueId()
        jdbcTemplate.update(
            "insert into members (member_id, name, signup_email, status, created_at) values (?, ?, ?, 'ACTIVE', now(6))",
            memberId,
            name,
            "$memberId@it.dpm",
        )
        return memberId
    }

    private fun joinTeam(
        memberId: Long,
        cohortId: CohortId,
        teamNumber: Int,
    ) {
        val teamId = uniqueId()
        jdbcTemplate.update(
            "insert into teams (team_id, number, cohort_id, created_at, updated_at) values (?, ?, ?, 0, 0)",
            teamId,
            teamNumber,
            cohortId.value,
        )
        jdbcTemplate.update("insert into member_teams (member_id, team_id) values (?, ?)", memberId, teamId)
    }

    private fun newSession(
        cohortId: CohortId,
        week: Int,
        isOnline: Boolean,
        deletedAt: Instant? = null,
    ): Session {
        val start = firstSessionStart.plus(Duration.ofDays(7L * (week - 1)))
        return sessionPort.save(
            Session(
                cohortId = cohortId,
                date = start,
                week = week,
                place = "온라인",
                eventName = "${week}주차 세션",
                isOnline = isOnline,
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = start.minus(Duration.ofMinutes(10)),
                        lateStart = start.plus(Duration.ofMinutes(15)),
                        absentStart = start.plus(Duration.ofMinutes(30)),
                        attendanceCode = "4321",
                    ),
                deletedAt = deletedAt,
            ),
        )
    }

    private fun addAttendance(
        session: Session,
        memberId: Long,
        status: AttendanceStatus,
    ) {
        attendancePort.save(Attendance(sessionId = session.id!!, memberId = MemberId(memberId), status = status))
    }

    private fun addAbsenceReason(
        session: Session,
        memberId: Long,
        contents: String,
        status: String,
    ) {
        jdbcTemplate.update(
            "insert into absence_reasons (session_id, member_id, contents, status, created_at) values (?, ?, ?, ?, now(6))",
            session.id!!.value,
            memberId,
            contents,
            status,
        )
    }

    private fun uniqueName(): String = "it579-" + UUID.randomUUID().toString().substring(0, 8)

    companion object {
        @JvmStatic
        @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
