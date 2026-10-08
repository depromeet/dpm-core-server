package core.it.attendance

import core.application.attendance.application.service.AttendanceCommandService
import core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService
import core.application.support.MutableClock
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
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
import java.sql.Timestamp
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

    @MockitoBean lateinit var feedbackForms: SessionFeedbackFormCommandService

    private val firstSessionStart = Instant.parse("2026-09-05T05:00:00Z")

    /** 다른 테스트의 고정 멤버 ID 와 겹치지 않도록 멤버/팀 ID 를 크게 잡는다. */
    private fun uniqueId(): Long = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L)

    @Test
    fun `사람별 목록은 현재 기수 소속 멤버 전원을 기록이 없어도 한 행씩 팀, 이름, ID 순으로 주고 살아 있는 최신 기록만 센다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val first = newSession(currentCohort, week = 1, isOnline = false)
        val second = newSession(currentCohort, week = 2, isOnline = true)
        val third = newSession(currentCohort, week = 3, isOnline = false)
        val deletedSession = newSession(currentCohort, week = 4, isOnline = false, deletedAt = firstSessionStart)
        val oldSession = newSession(oldCohort, week = 1, isOnline = true)

        // 이전/현재 기수 소속이고 현재 기수 소속·팀 매핑이 중복돼도 한 행이다
        val veteran = newMember("$name-b")
        joinCohort(veteran, oldCohort)
        joinCohort(veteran, currentCohort)
        joinCohort(veteran, currentCohort)
        joinTeam(veteran, oldCohort, teamNumber = 1)
        joinTeam(veteran, currentCohort, teamNumber = 2)
        joinTeam(veteran, currentCohort, teamNumber = 2)
        addAttendance(first, veteran, AttendanceStatus.ABSENT)
        addAttendance(second, veteran, AttendanceStatus.LATE)
        addAttendance(third, veteran, AttendanceStatus.ABSENT, deletedAt = firstSessionStart)
        addAttendance(deletedSession, veteran, AttendanceStatus.ABSENT)
        addAttendance(oldSession, veteran, AttendanceStatus.PRESENT)
        // 같은 세션의 살아 있는 기록이 둘이면 attendance_id 가 큰 쪽(출석)만 센다
        val duplicated = newMember("$name-a")
        joinCohort(duplicated, currentCohort)
        joinTeam(duplicated, currentCohort, teamNumber = 2)
        addAttendance(first, duplicated, AttendanceStatus.ABSENT)
        addAttendance(first, duplicated, AttendanceStatus.PRESENT)
        // 출석 기록이 없어도 집계 0 으로 나온다
        val noRecord = newMember("$name-c")
        joinCohort(noRecord, currentCohort)
        joinTeam(noRecord, currentCohort, teamNumber = 1)
        // 팀이 없으면 마지막이고 팀 0 이다
        val teamless = newMember("$name-0")
        joinCohort(teamless, currentCohort)
        addAttendance(first, teamless, AttendanceStatus.PRESENT)

        // 빠지는 멤버: 이전 기수만 소속(현재 기수 기록이 있어도), 삭제된 멤버, 소속 없이 기록만 있는 멤버
        val oldOnly = newMember("$name-old")
        joinCohort(oldOnly, oldCohort)
        addAttendance(first, oldOnly, AttendanceStatus.PRESENT)
        val deletedMember = newMember("$name-deleted")
        joinCohort(deletedMember, currentCohort)
        addAttendance(first, deletedMember, AttendanceStatus.PRESENT)
        jdbcTemplate.update("update members set deleted_at = now(6) where member_id = ?", deletedMember)
        addAttendance(first, newMember("$name-record-only"), AttendanceStatus.PRESENT)

        val rows = attendancePort.findMemberAttendances(currentCohort.value, emptyList())

        assertThat(rows.map { it.id }).containsExactly(noRecord, duplicated, veteran, teamless)
        assertThat(rows.map { it.teamNumber }).containsExactly(TeamNumber(1), TeamNumber(2), TeamNumber(2), TeamNumber(0))
        // 분모는 삭제되지 않은 현재 기수 세션 3개로 모두 같다
        assertThat(rows.map { it.summary }).containsExactly(
            summary(total = 3),
            summary(total = 3, present = 1),
            summary(total = 3, late = 1, offlineAbsent = 1),
            summary(total = 3, present = 1),
        )
        // 전체 수도 같은 대상이다: 소속 중복은 한 번, 기록 없음·팀 없음 포함, 이전 기수만·삭제·소속 없음 제외
        assertThat(attendancePort.countCohortMembers(currentCohort.value)).isEqualTo(4)

        // 이전 기수로 조회하면 이전 기수 소속과 그 기수 기록만 본다
        val oldRows = attendancePort.findMemberAttendances(oldCohort.value, emptyList())
        assertThat(oldRows.map { it.id }).containsExactlyInAnyOrder(veteran, oldOnly)
        assertThat(oldRows.single { it.id == veteran }.teamNumber).isEqualTo(TeamNumber(1))
        assertThat(oldRows.single { it.id == veteran }.summary).isEqualTo(summary(total = 1, present = 1))
        assertThat(oldRows.single { it.id == oldOnly }.summary).isEqualTo(summary(total = 1))
        assertThat(attendancePort.countCohortMembers(oldCohort.value)).isEqualTo(2)
    }

    @Test
    fun `사람별 목록의 팀 필터는 여러 팀을 받고 현재 기수 최신 배정 팀 기준이며 이전 팀이나 다른 기수 팀으로는 걸리지 않는다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val moved = newMember("$name-a")
        joinCohort(moved, currentCohort)
        joinTeam(moved, currentCohort, teamNumber = 5)
        joinTeam(moved, currentCohort, teamNumber = 7)
        val teamOne = newMember("$name-b")
        joinCohort(teamOne, currentCohort)
        joinTeam(teamOne, oldCohort, teamNumber = 7)
        joinTeam(teamOne, currentCohort, teamNumber = 1)
        val teamless = newMember("$name-c")
        joinCohort(teamless, currentCohort)
        joinTeam(teamless, oldCohort, teamNumber = 5)

        fun idsOf(vararg teams: Int) = attendancePort.findMemberAttendances(currentCohort.value, teams.toList()).map { it.id }

        assertThat(idsOf(5)).isEmpty()
        assertThat(idsOf(7)).containsExactly(moved)
        assertThat(idsOf(1, 7)).containsExactly(teamOne, moved)
        assertThat(idsOf(1, 5, 7, 9)).containsExactly(teamOne, moved)
        assertThat(idsOf()).containsExactly(teamOne, moved, teamless)
        // 전체 수는 팀 필터와 무관하게 팀 없는 멤버까지 센다
        assertThat(idsOf(7).size).isLessThan(attendancePort.countCohortMembers(currentCohort.value))
        assertThat(attendancePort.countCohortMembers(currentCohort.value)).isEqualTo(3)
    }

    @Test
    fun `사람별 상세와 세션별 상세는 목록과 같은 현재 기수 집계를 쓰고 세션별 기록과 결석 사유서를 붙인다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val memberId = newMember(name)
        joinCohort(memberId, oldCohort)
        joinCohort(memberId, currentCohort)
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
        // 같은 세션에 살아 있는 기록이 둘이면 attendance_id 가 큰 쪽(지각)만 본다. 삭제된 기록은 보지 않는다
        addAttendance(late, memberId, AttendanceStatus.ABSENT)
        addAttendance(late, memberId, AttendanceStatus.LATE)
        addAttendance(late, memberId, AttendanceStatus.EXCUSED_ABSENT, deletedAt = firstSessionStart)
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

        val expectedSummary = summary(total = 5, present = 1, late = 1, offlineAbsent = 1)

        val detail = attendancePort.findDetailMemberAttendance(memberId, currentCohort.value)!!
        // 표시 팀은 그 기수의 가장 최근 배정
        assertThat(detail.teamNumber).isEqualTo(TeamNumber(7))
        assertThat(detail.summary).isEqualTo(expectedSummary)
        // 목록 행과 같은 집계다
        assertThat(attendancePort.findMemberAttendances(currentCohort.value, emptyList()).single { it.id == memberId }.summary)
            .isEqualTo(expectedSummary)

        val sessions = attendancePort.findMemberSessionAttendances(memberId, currentCohort.value)
        assertThat(sessions.map { it.sessionId }).containsExactly(present.id!!.value, late.id!!.value, absent.id!!.value, upcoming.id!!.value)
        assertThat(sessions.map { it.sessionAttendanceStatus }).containsExactly("PRESENT", "LATE", "ABSENT", "PENDING")
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

        // 세션별 개인 상세도 같은 집계와 같은(최신) 기록을 쓴다. 중복 기록으로 행이 늘지 않는다
        val sessionDetail =
            attendancePort.findDetailAttendanceBySession(GetDetailAttendanceBySessionQuery(late.id!!, MemberId(memberId)))!!
        assertThat(sessionDetail.summary).isEqualTo(expectedSummary)
        assertThat(sessionDetail.teamNumber).isEqualTo(TeamNumber(7))
        assertThat(sessionDetail.attendanceStatus).isEqualTo("LATE")

        // 이전 기수 집계는 따로다
        assertThat(attendancePort.findDetailMemberAttendance(memberId, oldCohort.value)!!.summary)
            .isEqualTo(summary(total = 1, offlineAbsent = 1))
    }

    @Test
    fun `사람별 상세는 기록이 없는 현재 기수 멤버를 집계 0 으로 주고 소속이 아니거나 삭제·없는 멤버는 없다`() {
        val name = uniqueName()
        val (oldCohort, currentCohort) = newCohortPair()
        val session = newSession(currentCohort, week = 1, isOnline = false)
        newSession(currentCohort, week = 2, isOnline = false, deletedAt = firstSessionStart)

        val newcomer = newMember(name)
        joinCohort(newcomer, currentCohort)
        val pastOnly = newMember(name)
        joinCohort(pastOnly, oldCohort)
        addAttendance(session, pastOnly, AttendanceStatus.PRESENT)
        val notJoined = newMember(name)
        addAttendance(session, notJoined, AttendanceStatus.PRESENT)
        val deletedMember = newMember(name)
        joinCohort(deletedMember, currentCohort)
        addAttendance(session, deletedMember, AttendanceStatus.PRESENT)
        jdbcTemplate.update("update members set deleted_at = now(6) where member_id = ?", deletedMember)

        val detail = attendancePort.findDetailMemberAttendance(newcomer, currentCohort.value)!!
        assertThat(detail.memberId).isEqualTo(newcomer)
        assertThat(detail.teamNumber).isEqualTo(TeamNumber(0))
        assertThat(detail.summary).isEqualTo(summary(total = 1))
        assertThat(attendancePort.findMemberSessionAttendances(newcomer, currentCohort.value)).isEmpty()

        listOf(pastOnly, notJoined, deletedMember, uniqueId()).forEach { memberId ->
            assertThat(attendancePort.findDetailMemberAttendance(memberId, currentCohort.value)).isNull()
        }
    }

    @Test
    fun `운영진 단건·일괄 변경은 인증 시각을 지우고 인증 시각이 남은 예전 운영진 기록도 상세에서는 null 로 읽는다`() {
        val name = uniqueName()
        val (_, currentCohort) = newCohortPair()
        val first = newSession(currentCohort, week = 1, isOnline = false)
        val second = newSession(currentCohort, week = 2, isOnline = false)
        val attendedAt = firstSessionStart.minus(Duration.ofMinutes(3))
        val member = newMember("$name-a")
        val other = newMember("$name-b")
        listOf(member, other).forEach { joinCohort(it, currentCohort) }
        addAttendance(first, member, AttendanceStatus.PRESENT, attendedAt)
        addAttendance(first, other, AttendanceStatus.LATE, attendedAt)
        // 운영진 변경이 인증 시각을 지우기 전의 기록: 저장값은 그대로 두고 읽을 때만 가린다
        addAttendance(second, member, AttendanceStatus.LATE, attendedAt, updatedAt = firstSessionStart)

        assertThat(attendancePort.findMemberSessionAttendances(member, currentCohort.value).map { it.attendedAt })
            .containsExactly(attendedAt, null)
        val legacyDetail = attendancePort.findDetailAttendanceBySession(GetDetailAttendanceBySessionQuery(second.id!!, MemberId(member)))!!
        assertThat(legacyDetail.attendedAt).isNull()
        // updated_at 은 존을 가정하지 않고 저장한 시각 그대로 읽는다
        assertThat(legacyDetail.updatedAt).isEqualTo(firstSessionStart)
        assertThat(attendedAtInDb(second, member)).isNotNull()
        val untouchedDetail = attendancePort.findDetailAttendanceBySession(GetDetailAttendanceBySessionQuery(first.id!!, MemberId(member)))!!
        assertThat(untouchedDetail.attendedAt).isEqualTo(attendedAt)
        assertThat(untouchedDetail.updatedAt).isNull()

        // 내 세션 출석 상세도 같은 기준으로 가린다
        fun myAttendedAt(session: Session) = attendancePort.findMyDetailAttendanceBySession(GetMyAttendanceBySessionQuery(session.id!!, MemberId(member)))!!.attendedAt
        assertThat(myAttendedAt(second)).isNull()
        assertThat(myAttendedAt(first)).isEqualTo(attendedAt)

        // 단건: 저장된 인증 시각이 지워지고 변경 시각이 남는다
        attendanceCommandService.updateAttendanceStatus(AttendanceStatusUpdateCommand(first.id!!, MemberId(member), AttendanceStatus.EXCUSED_ABSENT))

        assertThat(attendedAtInDb(first, member)).isNull()
        val changed = attendancePort.findAttendanceBy(first.id!!.value, member)!!
        assertThat(changed.status).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(changed.updatedAt).isEqualTo(clock.now)
        val changedDetail = attendancePort.findDetailAttendanceBySession(GetDetailAttendanceBySessionQuery(first.id!!, MemberId(member)))!!
        assertThat(changedDetail.attendedAt).isNull()
        assertThat(changedDetail.updatedAt).isEqualTo(clock.now)
        assertThat(myAttendedAt(first)).isNull()
        assertThat(attendedAtInDb(first, other)).isNotNull()

        // 일괄: 대상 모두 지워진다
        attendanceCommandService.updateAttendanceStatusBulk(first.id!!, AttendanceStatus.ABSENT, listOf(MemberId(other), MemberId(member)))

        listOf(member, other).forEach { memberId ->
            assertThat(attendedAtInDb(first, memberId)).isNull()
            assertThat(attendancePort.findAttendanceBy(first.id!!.value, memberId)!!.status).isEqualTo(AttendanceStatus.ABSENT)
        }
        assertThat(attendancePort.findSessionRoster(first.id!!.value, currentCohort.value).map { it.attendedAt }).containsOnlyNulls()
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
        // 인증 시각이 남은 예전 운영진 변경 기록은 저장값 그대로 읽고 응답에서 가린다(매퍼)
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
    fun `명단은 멤버당 최신 살아 있는 기록 한 행이고 운영진 변경은 저장된 인증 시각을 지운다`() {
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

        // 운영진 단건 변경(PATCH 와 같은 서비스): 상태와 변경 시각이 바뀌고 저장된 인증 시각은 지워진다
        val changed = members.first()
        attendanceCommandService.updateAttendanceStatus(AttendanceStatusUpdateCommand(session.id!!, MemberId(changed), AttendanceStatus.ABSENT))

        val changedRow = attendancePort.findSessionRoster(session.id!!.value, currentCohort.value).single { it.memberId == changed }
        assertThat(changedRow.attendanceStatus).isEqualTo("ABSENT")
        assertThat(changedRow.attendedAt).isNull()
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

    /** 저장된 attended_at 원본. 조회 경로의 가림과 무관하게 지워졌는지 본다 */
    private fun attendedAtInDb(
        session: Session,
        memberId: Long,
    ): Timestamp? =
        jdbcTemplate.queryForObject(
            "select max(attended_at) from attendances where session_id = ? and member_id = ? and deleted_at is null",
            Timestamp::class.java,
            session.id!!.value,
            memberId,
        )

    private fun summary(
        total: Int,
        present: Int = 0,
        late: Int = 0,
        offlineAbsent: Int = 0,
    ) = AttendanceSummaryQueryModel(
        totalSessionCount = total,
        presentCount = present,
        lateCount = late,
        excusedAbsentCount = 0,
        onlineAbsentCount = 0,
        offlineAbsentCount = offlineAbsent,
    )

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
