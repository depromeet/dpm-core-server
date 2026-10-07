package core.application.session.application.service

import core.application.support.AttendanceTestFixture
import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.enums.SessionAttendanceStatus
import core.domain.session.vo.AttendancePolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class SessionQueryServiceTest {
    private val now = Instant.parse("2026-10-10T10:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)
    private val cohortId = fixture.createActiveCohort()

    @Test
    fun `v1 은 기존처럼 ID 순, v3 는 주차와 무관하게 일시 다음 ID 순이고 둘 다 현재 활성 기수만 준다`() {
        // 주차 순서와 일시 순서가 어긋나고, 같은 일시가 둘이다
        val laterButWeek1 = saveSession(week = 1, date = now.plus(Duration.ofDays(7)))
        val earlierButWeek3 = saveSession(week = 3, date = now, eventName = "네트워킹", place = "온라인", isOnline = true)
        val sameDateLowerId = saveSession(week = 5, date = now.plus(Duration.ofDays(1)))
        val sameDateHigherId = saveSession(week = 2, date = now.plus(Duration.ofDays(1)))
        saveSession(week = 1, date = now.minus(Duration.ofDays(1)), cohort = CohortId(cohortId.value + 100))

        val legacy = fixture.sessionQueryService.getSessionWeeks()
        val selector = fixture.sessionQueryService.getSessionSelector()

        assertThat(legacy.map { it.sessionId })
            .containsExactly(laterButWeek1.id, earlierButWeek3.id, sameDateLowerId.id, sameDateHigherId.id)
        assertThat(selector.map { it.sessionId })
            .containsExactly(earlierButWeek3.id, sameDateLowerId.id, sameDateHigherId.id, laterButWeek1.id)
        val first = selector.first()
        assertThat(listOf(first.week, first.date, first.eventName, first.place, first.isOnline))
            .containsExactly(3, now, "네트워킹", "온라인", true)
    }

    @Test
    fun `출석 인증 상태는 시작 전, 시작 정각부터 마감 전까지, 마감 정각부터로 나뉜다`() {
        val attendanceStart = now.plus(Duration.ofHours(1))
        val absentStart = attendanceStart.plus(Duration.ofMinutes(30))
        saveSession(week = 1, date = attendanceStart, attendanceStart = attendanceStart)

        val expectations =
            listOf(
                attendanceStart.minusSeconds(1) to SessionAttendanceStatus.NOT_STARTED,
                attendanceStart to SessionAttendanceStatus.IN_PROGRESS,
                // 지각 구간
                attendanceStart.plus(Duration.ofMinutes(20)) to SessionAttendanceStatus.IN_PROGRESS,
                absentStart.minusSeconds(1) to SessionAttendanceStatus.IN_PROGRESS,
                absentStart to SessionAttendanceStatus.CLOSED,
                absentStart.plus(Duration.ofDays(1)) to SessionAttendanceStatus.CLOSED,
            )

        expectations.forEach { (at, expected) ->
            fixture.clock.now = at
            val week = fixture.sessionQueryService.getSessionSelector().single()
            assertThat(week.attendanceStatus).describedAs("now=%s", at).isEqualTo(expected)
        }
    }

    private fun saveSession(
        week: Int,
        date: Instant,
        attendanceStart: Instant = date,
        eventName: String = "${week}주차 세션",
        place: String = "디프만 오프라인 장소",
        isOnline: Boolean = false,
        cohort: CohortId = cohortId,
    ): Session =
        fixture.sessions.save(
            Session(
                cohortId = cohort,
                date = date,
                week = week,
                place = place,
                eventName = eventName,
                isOnline = isOnline,
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = attendanceStart,
                        lateStart = attendanceStart.plus(Duration.ofMinutes(15)),
                        absentStart = attendanceStart.plus(Duration.ofMinutes(30)),
                        attendanceCode = AttendanceTestFixture.CODE,
                    ),
            ),
        )
}
