package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.session.application.exception.SessionNotFoundException
import core.application.support.AttendanceTestFixture
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import core.domain.team.vo.TeamNumber
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class AttendanceQueryServiceTest {
    private val now = Instant.parse("2026-10-10T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()
    private val adminId = MemberId(100)

    @Test
    fun `현재 기수 세션은 전체 명단과 팀 없는 멤버를 그대로 주고 내 팀은 현재 기수 팀이다`() {
        val session = saveSession(cohortId)
        fixture.attendances.rosters[session.id!!.value to cohortId.value] =
            (1L..28L).map { memberId -> rosterRow(memberId = memberId, teamNumber = if (memberId == 1L) null else 2) }
        fixture.attendances.cohortTeamNumbers[adminId.value to cohortId.value] = 2
        // 이전 기수 팀은 쓰지 않는다
        fixture.attendances.cohortTeamNumbers[adminId.value to cohortId.value + 100] = 5

        val roster = fixture.attendanceQueryService.getSessionRoster(session.id!!, adminId)

        assertThat(roster.members).hasSize(28)
        assertThat(roster.members.first().teamNumber).isNull()
        assertThat(roster.myTeamNumber).isEqualTo(2)
    }

    @Test
    fun `없거나 삭제됐거나 현재 활성 기수가 아닌 세션의 명단은 찾을 수 없다`() {
        val deleted = saveSession(cohortId, deletedAt = now)
        val otherCohortId = fixture.cohorts.save(Cohort(value = "17")).id!!
        val otherCohortSession = saveSession(otherCohortId)
        fixture.attendances.rosters[otherCohortSession.id!!.value to otherCohortId.value] = listOf(rosterRow(memberId = 1, teamNumber = 1))

        listOf(SessionId(999), deleted.id!!, otherCohortSession.id!!).forEach { sessionId ->
            assertThatThrownBy { fixture.attendanceQueryService.getSessionRoster(sessionId, adminId) }
                .isInstanceOf(SessionNotFoundException::class.java)
        }
    }

    @Test
    fun `사람별 목록은 활성 기수(최댓값 아님) 전원을 팀 필터만 적용해 그대로 주고 내 팀은 활성 기수 팀이다`() {
        val higherInactive = fixture.cohorts.save(Cohort(value = "19")).id!!
        fixture.attendances.memberAttendances[cohortId.value] =
            listOf(
                memberRow(id = 3, teamNumber = 1, summary = summary(total = 10, offlineAbsent = 2)),
                memberRow(id = 1, teamNumber = 2, summary = summary(total = 10)),
                // 출석 기록이 없어도 집계 0 으로 온다. 팀 없음은 0 으로 마지막
                memberRow(id = 2, teamNumber = 0, summary = summary(total = 10)),
            )
        fixture.attendances.memberAttendances[higherInactive.value] = listOf(memberRow(id = 9, teamNumber = 1, summary = summary(total = 1)))
        fixture.attendances.cohortTeamNumbers[adminId.value to cohortId.value] = 2
        fixture.attendances.cohortTeamNumbers[adminId.value to higherInactive.value] = 5

        val all = fixture.attendanceQueryService.getMemberAttendances(GetMemberAttendancesQuery(adminId, teams = null))

        // 조회 결과 순서를 다시 정렬하지 않는다
        assertThat(all.members.map { it.id }).containsExactly(3L, 1L, 2L)
        assertThat(all.members.map { it.teamNumber }).containsExactly(TeamNumber(1), TeamNumber(2), TeamNumber(0))
        assertThat(all.members.map { it.attendanceStatus }).containsExactly("AT_RISK", "NORMAL", "NORMAL")
        assertThat(all.myTeamNumber).isEqualTo(2)
        assertThat(all.totalElements).isEqualTo(3)

        assertThat(fixture.attendanceQueryService.getMemberAttendances(GetMemberAttendancesQuery(adminId, emptyList())).members)
            .hasSize(3)
        val filtered = fixture.attendanceQueryService.getMemberAttendances(GetMemberAttendancesQuery(adminId, listOf(1, 2)))
        assertThat(filtered.members.map { it.id }).containsExactly(3L, 1L)
        // 전체 수는 팀 필터와 무관하다
        assertThat(filtered.totalElements).isEqualTo(3)
    }

    @Test
    fun `사람별 목록의 내 팀은 활성 기수에 팀이 없으면 null 이다`() {
        fixture.attendances.cohortTeamNumbers[adminId.value to cohortId.value + 100] = 5

        val response = fixture.attendanceQueryService.getMemberAttendances(GetMemberAttendancesQuery(adminId, teams = null))

        assertThat(response.members).isEmpty()
        assertThat(response.myTeamNumber).isNull()
        assertThat(response.totalElements).isZero()
    }

    @Test
    fun `사람별 상세는 활성 기수 기준이고 기록이 없는 소속 멤버는 집계 0, 세션 없이 NORMAL 이다`() {
        val memberId = MemberId(7)
        fixture.attendances.memberDetails[memberId.value to cohortId.value] = memberDetail(memberId.value, summary(total = 6))
        // 이전 기수 기록은 보지 않는다
        val pastCohort = fixture.cohorts.save(Cohort(value = "17")).id!!
        fixture.attendances.memberDetails[memberId.value to pastCohort.value] =
            memberDetail(memberId.value, summary(total = 6, offlineAbsent = 3))

        val detail = fixture.attendanceQueryService.getDetailMemberAttendances(GetDetailMemberAttendancesQuery(memberId))

        assertThat(detail.member.id).isEqualTo(7L)
        assertThat(detail.member.attendanceStatus).isEqualTo("NORMAL")
        assertThat(listOf(detail.attendance.presentCount, detail.attendance.lateCount, detail.attendance.excusedAbsentCount, detail.attendance.absentCount))
            .containsOnly(0)
        assertThat(detail.sessions).isEmpty()
    }

    @Test
    fun `사람별 상세는 현재 기수 소속이 아니거나 없는 멤버면 출석 없음이다`() {
        val pastOnly = MemberId(8)
        val pastCohort = fixture.cohorts.save(Cohort(value = "17")).id!!
        fixture.attendances.memberDetails[pastOnly.value to pastCohort.value] = memberDetail(pastOnly.value, summary(total = 6))

        listOf(pastOnly, MemberId(404)).forEach { memberId ->
            assertThatThrownBy { fixture.attendanceQueryService.getDetailMemberAttendances(GetDetailMemberAttendancesQuery(memberId)) }
                .isInstanceOf(AttendanceNotFoundException::class.java)
        }
    }

    private fun memberRow(
        id: Long,
        teamNumber: Int,
        summary: AttendanceSummaryQueryModel,
    ) = MemberAttendanceQueryModel(
        id = id,
        name = "멤버$id",
        teamNumber = TeamNumber(teamNumber),
        isAdmin = false,
        part = "SERVER",
        summary = summary,
    )

    private fun memberDetail(
        memberId: Long,
        summary: AttendanceSummaryQueryModel,
    ) = MemberDetailAttendanceQueryModel(
        memberId = memberId,
        memberName = "멤버$memberId",
        teamNumber = TeamNumber(0),
        isAdmin = false,
        part = null,
        summary = summary,
    )

    private fun summary(
        total: Int,
        offlineAbsent: Int = 0,
    ) = AttendanceSummaryQueryModel(
        totalSessionCount = total,
        presentCount = 0,
        lateCount = 0,
        excusedAbsentCount = 0,
        onlineAbsentCount = 0,
        offlineAbsentCount = offlineAbsent,
    )

    private fun saveSession(
        cohort: CohortId,
        deletedAt: Instant? = null,
    ): Session =
        fixture.sessions.save(
            Session(
                cohortId = cohort,
                date = now,
                week = 1,
                place = "온라인",
                eventName = "1주차 세션",
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = now,
                        lateStart = now.plus(Duration.ofMinutes(15)),
                        absentStart = now.plus(Duration.ofMinutes(30)),
                        attendanceCode = AttendanceTestFixture.CODE,
                    ),
                deletedAt = deletedAt,
            ),
        )

    private fun rosterRow(
        memberId: Long,
        teamNumber: Int?,
    ) = SessionRosterQueryModel(
        memberId = memberId,
        name = "멤버$memberId",
        teamNumber = teamNumber,
        isAdmin = false,
        part = "SERVER",
        attendanceStatus = "PENDING",
        attendedAt = null,
        updatedAt = null,
        absenceReason = null,
    )
}
