package core.application.attendance.application.service

import core.application.session.application.exception.SessionNotFoundException
import core.application.support.AttendanceTestFixture
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
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
