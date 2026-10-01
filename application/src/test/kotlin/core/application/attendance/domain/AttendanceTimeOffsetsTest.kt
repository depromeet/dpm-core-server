package core.application.attendance.domain

import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.attendance.vo.AttendanceTimeOffsets.Violation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class AttendanceTimeOffsetsTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")

    @Test
    fun `초기값은 10_15_30 분이다`() {
        assertThat(AttendanceTimeOffsets.DEFAULT)
            .isEqualTo(AttendanceTimeOffsets(10, 15, 30))
    }

    @Test
    fun `정책은 세션 시작 기준 절대 시각으로 확정된다`() {
        val times = AttendanceTimeOffsets(20, 5, 45).resolveFor(sessionStart)

        assertThat(times.attendanceStart).isEqualTo(sessionStart.minusSeconds(20 * 60))
        assertThat(times.lateStart).isEqualTo(sessionStart.plusSeconds(5 * 60))
        assertThat(times.absentStart).isEqualTo(sessionStart.plusSeconds(45 * 60))
    }

    @Test
    fun `경계값 0 과 1440 은 허용한다`() {
        assertThat(AttendanceTimeOffsets.violationOf(0, 1, 2)).isNull()
        assertThat(AttendanceTimeOffsets.violationOf(1, 0, 1)).isNull()
        assertThat(AttendanceTimeOffsets.violationOf(1440, 1439, 1440)).isNull()
        assertThat(AttendanceTimeOffsets(1440, 0, 1440).resolveFor(sessionStart).attendanceStart)
            .isEqualTo(sessionStart.minusSeconds(86_400))
    }

    @Test
    fun `음수나 1440 초과는 범위 오류다`() {
        assertThat(AttendanceTimeOffsets.violationOf(-1, 15, 30)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(10, -1, 30)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(10, 15, -30)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(1441, 15, 30)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(10, 15, 1441)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(Int.MAX_VALUE, 15, 30)).isEqualTo(Violation.OUT_OF_RANGE)
        assertThat(AttendanceTimeOffsets.violationOf(10, Int.MIN_VALUE, 30)).isEqualTo(Violation.OUT_OF_RANGE)
    }

    @Test
    fun `지각 시작이 마감과 같거나 늦으면 순서 오류다`() {
        assertThat(AttendanceTimeOffsets.violationOf(10, 30, 30)).isEqualTo(Violation.NOT_ORDERED)
        assertThat(AttendanceTimeOffsets.violationOf(10, 31, 30)).isEqualTo(Violation.NOT_ORDERED)
        assertThat(AttendanceTimeOffsets.violationOf(10, 29, 30)).isNull()
    }

    @Test
    fun `인증 시작과 지각 시작이 같은 시각이면 순서 오류다`() {
        assertThat(AttendanceTimeOffsets.violationOf(0, 0, 30)).isEqualTo(Violation.NOT_ORDERED)
    }

    @Test
    fun `잘못된 값으로는 값 객체를 만들 수 없다`() {
        assertThatThrownBy { AttendanceTimeOffsets(10, 30, 15) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { AttendanceTimeOffsets(-1, 15, 30) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
