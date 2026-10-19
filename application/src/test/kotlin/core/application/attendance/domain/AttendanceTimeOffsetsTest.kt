package core.application.attendance.domain

import core.domain.attendance.vo.AttendanceTimeOffsets
import core.domain.attendance.vo.AttendanceTimeOffsets.Violation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AttendanceTimeOffsetsTest {
    private val sessionStart = Instant.parse("2026-10-10T10:00:00Z")

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
    fun `범위와 순서를 위반하는 값은 거절한다`() {
        val cases =
            listOf(
                Triple(-1, 15, 30) to Violation.OUT_OF_RANGE,
                Triple(10, -1, 30) to Violation.OUT_OF_RANGE,
                Triple(10, 15, 1441) to Violation.OUT_OF_RANGE,
                Triple(Int.MAX_VALUE, 15, 30) to Violation.OUT_OF_RANGE,
                Triple(10, Int.MIN_VALUE, 30) to Violation.OUT_OF_RANGE,
                Triple(10, 30, 30) to Violation.NOT_ORDERED,
                Triple(10, 31, 30) to Violation.NOT_ORDERED,
                Triple(0, 0, 30) to Violation.NOT_ORDERED,
            )

        cases.forEach { (minutes, expected) ->
            val (open, late, absent) = minutes
            assertThat(AttendanceTimeOffsets.violationOf(open, late, absent)).describedAs("$minutes").isEqualTo(expected)
        }
    }
}
