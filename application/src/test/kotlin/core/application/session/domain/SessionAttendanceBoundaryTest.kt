package core.application.session.domain

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceResult
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class SessionAttendanceBoundaryTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)
    private val session =
        Session(
            id = SessionId(1L),
            cohortId = CohortId(18L),
            date = sessionStart,
            week = 1,
            place = "온라인",
            eventName = "1주차 세션",
            attendancePolicy =
                AttendancePolicy(
                    attendanceStart = times.attendanceStart,
                    lateStart = times.lateStart,
                    absentStart = times.absentStart,
                    attendanceCode = "1234",
                ),
        )

    @Test
    fun `인증 시작 1ns 전은 너무 이르고 정확히 인증 시작부터 출석이다`() {
        assertThat(session.attend(times.attendanceStart.minusNanos(1))).isEqualTo(AttendanceResult.TooEarly)
        assertThat(session.attend(times.attendanceStart)).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
    }

    @Test
    fun `지각 시작 1ns 전은 출석이고 정확히 지각 시작부터 지각이다`() {
        assertThat(session.attend(times.lateStart.minusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
        assertThat(session.attend(times.lateStart)).isEqualTo(AttendanceResult.Success(AttendanceStatus.LATE))
    }

    @Test
    fun `마감 1ns 전은 지각이고 정확히 마감부터는 결석 저장이 아니라 마감이다`() {
        assertThat(session.attend(times.absentStart.minusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.LATE))
        assertThat(session.attend(times.absentStart)).isEqualTo(AttendanceResult.Closed)
        assertThat(session.attend(times.absentStart.plusSeconds(86_400))).isEqualTo(AttendanceResult.Closed)

        assertThat(session.isAttendanceClosedAt(times.absentStart.minusNanos(1))).isFalse()
        assertThat(session.isAttendanceClosedAt(times.absentStart)).isTrue()
    }
}
