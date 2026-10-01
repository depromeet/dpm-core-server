package core.application.session.domain

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceResult
import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionAttendanceTimes
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SessionAttendanceBoundaryTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z") // 19:00 KST
    private val times = AttendanceTimeOffsets.DEFAULT.resolveFor(sessionStart)
    private val session = session(times)

    @Test
    fun `기본 정책은 T-10 부터 출석, T+15 부터 지각, T+30 부터 마감이다`() {
        assertThat(times.attendanceStart).isEqualTo(sessionStart.minusSeconds(600))
        assertThat(times.lateStart).isEqualTo(sessionStart.plusSeconds(900))
        assertThat(times.absentStart).isEqualTo(sessionStart.plusSeconds(1800))
    }

    @Test
    fun `인증 시작 1ns 전은 너무 이르고 정확히 인증 시작부터 출석이다`() {
        assertThat(session.attend(times.attendanceStart.minusNanos(1))).isEqualTo(AttendanceResult.TooEarly)
        assertThat(session.attend(times.attendanceStart)).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
        assertThat(session.attend(times.attendanceStart.plusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
    }

    @Test
    fun `지각 시작 1ns 전은 출석이고 정확히 지각 시작부터 지각이다`() {
        assertThat(session.attend(times.lateStart.minusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
        assertThat(session.attend(times.lateStart)).isEqualTo(AttendanceResult.Success(AttendanceStatus.LATE))
        assertThat(session.attend(times.lateStart.plusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.LATE))
    }

    @Test
    fun `마감 1ns 전은 지각이고 정확히 마감부터는 결석 저장이 아니라 마감이다`() {
        assertThat(session.attend(times.absentStart.minusNanos(1))).isEqualTo(AttendanceResult.Success(AttendanceStatus.LATE))
        assertThat(session.attend(times.absentStart)).isEqualTo(AttendanceResult.Closed)
        assertThat(session.attend(times.absentStart.plusNanos(1))).isEqualTo(AttendanceResult.Closed)
        assertThat(session.attend(times.absentStart.plusSeconds(86_400))).isEqualTo(AttendanceResult.Closed)
    }

    @Test
    fun `인증 결과는 절대 ABSENT 성공이 아니다`() {
        var t = times.attendanceStart.minusSeconds(60)
        while (t.isBefore(times.absentStart.plusSeconds(60))) {
            assertThat(session.attend(t)).isNotEqualTo(AttendanceResult.Success(AttendanceStatus.ABSENT))
            t = t.plusSeconds(7)
        }
    }

    @Test
    fun `마감 여부는 마감 시각을 포함한다`() {
        assertThat(session.isAttendanceClosedAt(times.absentStart.minusNanos(1))).isFalse()
        assertThat(session.isAttendanceClosedAt(times.absentStart)).isTrue()
        assertThat(session.isAttendanceClosedAt(times.absentStart.plusNanos(1))).isTrue()
    }

    @Test
    fun `자정 직후 세션의 T-10 인증 시작은 전날이어도 유효하다`() {
        val midnightSession = LocalDateTime.of(2026, 10, 11, 0, 5).atZone(KST).toInstant()
        val overnight = AttendanceTimeOffsets.DEFAULT.resolveFor(midnightSession)

        assertThat(overnight.attendanceStart.atZone(KST).toLocalDateTime())
            .isEqualTo(LocalDateTime.of(2026, 10, 10, 23, 55))

        val target = session(overnight)
        val newStart = LocalDateTime.of(2026, 10, 10, 23, 50).atZone(KST).toInstant()
        target.updateAttendanceStartTime(newStart)

        assertThat(target.attendancePolicy.attendanceStart).isEqualTo(newStart)
        assertThat(target.attend(newStart)).isEqualTo(AttendanceResult.Success(AttendanceStatus.PRESENT))
    }

    @Test
    fun `인증 시작을 지각 시작 이후로 바꾸면 거절한다`() {
        val target = session(times)

        assertThatThrownBy { target.updateAttendanceStartTime(times.lateStart) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(target.attendancePolicy.attendanceStart).isEqualTo(times.attendanceStart)
    }

    @Test
    fun `세션 수정은 출석 시각 순서를 검증한다`() {
        val target = session(times)

        assertThatThrownBy {
            target.updateSession(updateCommand(times.attendanceStart, times.absentStart, times.lateStart))
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            target.updateSession(updateCommand(times.attendanceStart, times.lateStart, times.lateStart))
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(target.attendancePolicy.absentStart).isEqualTo(times.absentStart)
    }

    @Test
    fun `세션 생성은 확정된 출석 시각만 사용한다`() {
        val explicit =
            SessionAttendanceTimes(
                attendanceStart = sessionStart,
                lateStart = sessionStart.plusSeconds(1200),
                absentStart = sessionStart.plusSeconds(2100),
            )
        val created =
            Session.create(
                SessionCreateCommand(
                    date = sessionStart,
                    week = 3,
                    place = null,
                    eventName = null,
                    isOnline = null,
                ),
                CohortId(18L),
                explicit,
            )

        assertThat(created.attendancePolicy.attendanceStart).isEqualTo(explicit.attendanceStart)
        assertThat(created.attendancePolicy.lateStart).isEqualTo(explicit.lateStart)
        assertThat(created.attendancePolicy.absentStart).isEqualTo(explicit.absentStart)
        assertThat(created.attendancePolicy.attendanceCode).matches("\\d{4}")
    }

    @Test
    fun `출석 시각 값 객체는 순서가 맞지 않으면 만들 수 없다`() {
        assertThatThrownBy { SessionAttendanceTimes(sessionStart, sessionStart, sessionStart.plusSeconds(1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SessionAttendanceTimes(sessionStart, sessionStart.plusSeconds(2), sessionStart.plusSeconds(1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(SessionAttendanceTimes.isOrdered(sessionStart, sessionStart.plusNanos(1), sessionStart.plusNanos(2))).isTrue()
    }

    private fun updateCommand(
        attendanceStart: Instant,
        lateStart: Instant,
        absentStart: Instant,
    ) = SessionUpdateCommand(
        sessionId = SessionId(1L),
        date = sessionStart,
        week = 1,
        place = null,
        eventName = null,
        isOnline = null,
        attendanceStart = attendanceStart,
        lateStart = lateStart,
        absentStart = absentStart,
    )

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")

        fun session(
            times: SessionAttendanceTimes,
            id: Long = 1L,
        ): Session =
            Session(
                id = SessionId(id),
                cohortId = CohortId(18L),
                date = times.lateStart,
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
    }
}
