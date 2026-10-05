package core.it.attendance

import core.application.attendance.application.service.AttendanceCommandService
import core.application.support.MutableClock
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
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

    @Autowired lateinit var attendanceCommandService: AttendanceCommandService

    @Autowired lateinit var clock: MutableClock

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
        val attendedAt = firstSessionStart.minus(Duration.ofMinutes(3))
        addAttendance(present, memberId, AttendanceStatus.PRESENT, attendedAt)
        addAttendance(late, memberId, AttendanceStatus.LATE)
        addAttendance(absent, memberId, AttendanceStatus.ABSENT)
        addAttendance(upcoming, memberId, AttendanceStatus.PENDING)
        addAttendance(deleted, memberId, AttendanceStatus.ABSENT)
        val oldReasonId = addAbsenceReason(absent, memberId, "예전 사유", "REJECTED")
        val reasonId = addAbsenceReason(absent, memberId, "병원 진료", "PENDING")
        val imageBase = uniqueId()
        // 최신 사유서의 첨부만, 표시 순서대로 붙는다. 파일명은 저장된 이미지에서, 없으면 null
        newImage(imageBase + 2, memberId, originalFileName = "진단서.jpg")
        newImage(imageBase + 1, memberId, originalFileName = null)
        linkImage(reasonId, imageBase + 2, displayOrder = 0)
        linkImage(reasonId, imageBase + 1, displayOrder = 1)
        linkImage(oldReasonId, imageBase + 3, displayOrder = 0)

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
        assertThat(sessions.map { it.sessionPlace }).containsExactly("1주차 장소", "", "3주차 장소", "4주차 장소")
        assertThat(sessions.map { it.attendedAt }).containsExactly(attendedAt, null, null, null)
        assertThat(sessions.map { it.absenceReason?.contents }).containsExactly(null, null, "병원 진료", null)
        assertThat(sessions.single { it.sessionId == absent.id!!.value }.absenceReason)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(
                MemberSessionAttendanceQueryModel.AbsenceReason(
                    id = 0,
                    contents = "병원 진료",
                    status = "PENDING",
                    images =
                        listOf(
                            MemberSessionAttendanceQueryModel.Image(imageBase + 2, "진단서.jpg"),
                            MemberSessionAttendanceQueryModel.Image(imageBase + 1, null),
                        ),
                ),
            )

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

    @Test
    fun `세션 명단은 현재 기수 소속의 살아 있는 출석 기록만 멤버당 한 행으로 팀, 이름, ID 순으로 준다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val session = newSession(currentCohort, week = 1, isOnline = false)
        val attendedAt = firstSessionStart.minus(Duration.ofMinutes(3))

        // 이전/현재 기수 모두 소속이고 이전 기수 팀과 현재 기수 팀 두 번 배정: 최신 배정 팀으로 한 번만 나온다
        val veteran = newMember("$name-b")
        joinCohort(veteran, oldCohort)
        joinCohort(veteran, currentCohort)
        joinTeam(veteran, oldCohort, teamNumber = 1)
        joinTeam(veteran, currentCohort, teamNumber = 5)
        joinTeam(veteran, currentCohort, teamNumber = 2)
        addAttendance(session, veteran, AttendanceStatus.PRESENT, attendedAt)
        // 같은 팀, 이름순으로 앞선다
        val sameTeam = newMember("$name-a")
        joinCohort(sameTeam, currentCohort)
        joinTeam(sameTeam, currentCohort, teamNumber = 2)
        addAttendance(session, sameTeam, AttendanceStatus.ABSENT, attendedAt, updatedAt = firstSessionStart)
        // 팀이 없어도 남고 마지막에 온다
        val teamless = newMember("$name-0")
        joinCohort(teamless, currentCohort)
        addAttendance(session, teamless, AttendanceStatus.EXCUSED_ABSENT)
        addAbsenceReason(session, teamless, "예전 사유", "REJECTED")
        addAbsenceReason(session, teamless, "병원 진료", "PENDING")
        // 앞 팀 번호
        val firstTeam = newMember("$name-z")
        joinCohort(firstTeam, currentCohort)
        joinTeam(firstTeam, currentCohort, teamNumber = 1)
        addAttendance(session, firstTeam, AttendanceStatus.PENDING)

        // 빠지는 행: 이전 기수만 소속, 삭제된 멤버, 삭제된 출석 기록, 없는 멤버(고아 기록)
        val oldOnly = newMember("$name-old")
        joinCohort(oldOnly, oldCohort)
        addAttendance(session, oldOnly, AttendanceStatus.PRESENT)
        val deletedMember = newMember("$name-deleted")
        joinCohort(deletedMember, currentCohort)
        addAttendance(session, deletedMember, AttendanceStatus.PRESENT)
        jdbcTemplate.update("update members set deleted_at = now(6) where member_id = ?", deletedMember)
        val deletedAttendance = newMember("$name-deleted-attendance")
        joinCohort(deletedAttendance, currentCohort)
        addAttendance(session, deletedAttendance, AttendanceStatus.PRESENT, deletedAt = firstSessionStart)
        addAttendance(session, uniqueId(), AttendanceStatus.PRESENT)

        val roster = attendancePort.findSessionRoster(session.id!!.value, currentCohort.value)

        assertThat(roster.map { it.memberId }).containsExactly(firstTeam, sameTeam, veteran, teamless)
        assertThat(roster.map { it.teamNumber }).containsExactly(1, 2, 2, null)
        assertThat(roster.map { it.attendanceStatus }).containsExactly("PENDING", "ABSENT", "PRESENT", "EXCUSED_ABSENT")
        // 운영진 변경 기록도 저장된 인증 시각은 그대로다
        assertThat(roster.map { it.attendedAt }).containsExactly(null, attendedAt, attendedAt, null)
        assertThat(roster.map { it.updatedAt }).containsExactly(null, firstSessionStart, null, null)
        assertThat(roster.map { it.absenceReason }).containsExactly(null, null, null, "병원 진료")

        // 세션 기수가 아닌 기수로 조회하면 비어 있다
        assertThat(attendancePort.findSessionRoster(session.id!!.value, oldCohort.value)).isEmpty()
        // 삭제된 세션은 비어 있다
        val deletedSession = newSession(currentCohort, week = 2, isOnline = false, deletedAt = firstSessionStart)
        addAttendance(deletedSession, veteran, AttendanceStatus.PRESENT)
        assertThat(attendancePort.findSessionRoster(deletedSession.id!!.value, currentCohort.value)).isEmpty()
    }

    @Test
    fun `명단은 멤버당 최신 살아 있는 기록 한 행이고 운영진 변경 뒤에도 저장된 인증 시각을 남긴다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val session = newSession(currentCohort, week = 1, isOnline = false)
        val attendedAt = firstSessionStart.minus(Duration.ofMinutes(3))

        // 22명: 출석 15(팀 있음 10, 팀 없음 5), 지각 4, 미인증 3
        val members =
            (1..22).map { index ->
                newMember("$name-${index.toString().padStart(2, '0')}").also { memberId ->
                    joinCohort(memberId, currentCohort)
                    if (index <= 10 || index > 15) joinTeam(memberId, currentCohort, teamNumber = index % 3 + 1)
                }
            }
        members.take(15).forEach { addAttendance(session, it, AttendanceStatus.PRESENT, attendedAt) }
        members.drop(15).take(4).forEach { addAttendance(session, it, AttendanceStatus.LATE, attendedAt) }
        members.drop(19).forEach { addAttendance(session, it, AttendanceStatus.PENDING) }
        // 같은 멤버의 살아 있는 기록이 둘이면 attendance_id 가 큰 쪽(지각)만 본다. 이전 기수 소속·팀 중복도 행을 늘리지 않는다
        val duplicated = newMember("$name-dup")
        joinCohort(duplicated, oldCohort)
        joinCohort(duplicated, currentCohort)
        joinCohort(duplicated, currentCohort)
        joinTeam(duplicated, oldCohort, teamNumber = 1)
        joinTeam(duplicated, currentCohort, teamNumber = 1)
        joinTeam(duplicated, currentCohort, teamNumber = 1)
        addAttendance(session, duplicated, AttendanceStatus.ABSENT)
        addAttendance(session, duplicated, AttendanceStatus.LATE, attendedAt)
        // 빠지는 기록: 이전 기수만 소속, 삭제된 기록
        val oldOnly = newMember("$name-old")
        joinCohort(oldOnly, oldCohort)
        addAttendance(session, oldOnly, AttendanceStatus.ABSENT)
        addAttendance(session, members.first(), AttendanceStatus.ABSENT, deletedAt = firstSessionStart)
        // 다른 기수 세션의 기록은 섞이지 않는다
        val otherSession = newSession(oldCohort, week = 1, isOnline = false)
        addAttendance(otherSession, oldOnly, AttendanceStatus.ABSENT)
        addAttendance(otherSession, duplicated, AttendanceStatus.EXCUSED_ABSENT)

        val roster = attendancePort.findSessionRoster(session.id!!.value, currentCohort.value)

        assertThat(roster.map { it.memberId }).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(members + duplicated)
        assertThat(roster.single { it.memberId == duplicated }.attendanceStatus).isEqualTo("LATE")
        assertThat(roster.count { it.teamNumber == null }).isEqualTo(5)
        assertThat(roster.groupingBy { it.attendanceStatus }.eachCount())
            .containsExactlyInAnyOrderEntriesOf(mapOf("PRESENT" to 15, "LATE" to 5, "PENDING" to 3))
        assertThat(attendancePort.findSessionRoster(otherSession.id!!.value, currentCohort.value)).isEmpty()
        assertThat(attendancePort.findSessionRoster(otherSession.id!!.value, oldCohort.value).map { it.memberId })
            .containsExactlyInAnyOrder(oldOnly, duplicated)

        // 운영진 단건 변경(PATCH 와 같은 서비스): 상태와 변경 시각만 바뀌고 저장된 인증 시각은 남는다
        val changed = members.first()
        attendanceCommandService.updateAttendanceStatus(AttendanceStatusUpdateCommand(session.id!!, MemberId(changed), AttendanceStatus.ABSENT))

        val changedRow = attendancePort.findSessionRoster(session.id!!.value, currentCohort.value).single { it.memberId == changed }
        assertThat(changedRow.attendanceStatus).isEqualTo("ABSENT")
        assertThat(changedRow.attendedAt).isEqualTo(attendedAt)
        assertThat(changedRow.updatedAt).isEqualTo(clock.now)
    }

    @Test
    fun `내 팀 번호는 그 기수의 최신 배정이고 없으면 null 이다`() {
        val (oldCohort, currentCohort) = newCohortPair()
        val member = newMember(uniqueName())
        // 최신 배정은 2, 이전 기수 팀 9 는 섞이지 않는다
        joinTeam(member, currentCohort, teamNumber = 3)
        joinTeam(member, currentCohort, teamNumber = 1)
        joinTeam(member, currentCohort, teamNumber = 3)
        joinTeam(member, currentCohort, teamNumber = 2)
        joinTeam(member, oldCohort, teamNumber = 9)
        val teamless = newMember(uniqueName())
        joinTeam(teamless, oldCohort, teamNumber = 9)

        assertThat(attendancePort.findTeamNumberInCohort(member, currentCohort.value)).isEqualTo(2)
        assertThat(attendancePort.findTeamNumberInCohort(teamless, currentCohort.value)).isNull()
    }

    @Test
    fun `기수 팀 목록은 멤버 배정과 무관하게 그 기수 팀 전부를 번호, ID 순으로 준다`() {
        val (oldCohort, currentCohort) = newCohortPair()
        val base = uniqueId()
        // 팀 7, 같은 번호 두 팀(ID 로 정렬), 멤버가 없는 팀 모두 포함. 다른 기수 팀은 섞이지 않는다
        newTeam(base + 5, currentCohort, number = 7)
        newTeam(base + 4, currentCohort, number = 2)
        newTeam(base + 3, currentCohort, number = 1)
        newTeam(base + 2, currentCohort, number = 2)
        newTeam(base + 9, oldCohort, number = 1)
        jdbcTemplate.update("insert into member_teams (member_id, team_id) values (?, ?)", newMember(uniqueName()), base + 4)

        assertThat(cohortPort.findTeamsByCohortId(currentCohort).map { it.id to it.number })
            .containsExactly(base + 3 to 1, base + 2 to 2, base + 4 to 2, base + 5 to 7)
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

    private fun joinCohort(
        memberId: Long,
        cohortId: CohortId,
    ) {
        jdbcTemplate.update("insert into member_cohorts (member_id, cohort_id) values (?, ?)", memberId, cohortId.value)
    }

    private fun newTeam(
        teamId: Long,
        cohortId: CohortId,
        number: Int,
    ) {
        jdbcTemplate.update(
            "insert into teams (team_id, number, cohort_id, created_at, updated_at) values (?, ?, ?, 0, 0)",
            teamId,
            number,
            cohortId.value,
        )
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
                place = if (isOnline) "" else "${week}주차 장소",
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
        attendedAt: Instant? = null,
        updatedAt: Instant? = null,
        deletedAt: Instant? = null,
    ) {
        attendancePort.save(
            Attendance(
                sessionId = session.id!!,
                memberId = MemberId(memberId),
                status = status,
                attendedAt = attendedAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
            ),
        )
    }

    private fun addAbsenceReason(
        session: Session,
        memberId: Long,
        contents: String,
        status: String,
    ): Long {
        jdbcTemplate.update(
            "insert into absence_reasons (session_id, member_id, contents, status, created_at) values (?, ?, ?, ?, now(6))",
            session.id!!.value,
            memberId,
            contents,
            status,
        )
        return jdbcTemplate.queryForObject("select max(absence_reason_id) from absence_reasons where member_id = ?", Long::class.javaObjectType, memberId)!!
    }

    private fun newImage(
        imageId: Long,
        ownerMemberId: Long,
        originalFileName: String?,
    ) {
        jdbcTemplate.update(
            "insert into images (image_id, owner_member_id, object_key, content_type, size_bytes, original_file_name, created_at) " +
                "values (?, ?, ?, 'image/jpeg', 1, ?, now(6))",
            imageId,
            ownerMemberId,
            "images/" + UUID.randomUUID(),
            originalFileName,
        )
    }

    private fun linkImage(
        absenceReasonId: Long,
        imageId: Long,
        displayOrder: Int,
    ) {
        jdbcTemplate.update(
            "insert into absence_reason_images (absence_reason_id, image_id, display_order) values (?, ?, ?)",
            absenceReasonId,
            imageId,
            displayOrder,
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
