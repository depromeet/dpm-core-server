package core.domain.attendance.vo

import core.domain.session.vo.SessionAttendanceTimes
import java.time.Duration
import java.time.Instant

/**
 * 세션 시작 시각(T)을 기준으로 한 기본 출석 시간(분 단위)입니다. 서버 설정(attendance.policy)에서 읽습니다.
 *
 * - 인증 시작: T - [attendanceOpenMinutesBeforeStart] (포함)
 * - 지각 시작: T + [lateAfterStartMinutes] (포함)
 * - 인증 마감: T + [absentAfterStartMinutes] (포함, 이 시각부터 인증 거절 및 자동 결석 대상)
 *
 * 모든 값은 [MIN_MINUTES] 이상 [MAX_MINUTES] 이하이며,
 * 인증 시작 < 지각 시작 < 인증 마감 이 되도록 지각 시작은 마감보다 작아야 하고
 * 인증 시작과 지각 시작이 같은 시각(두 값이 모두 0)이 될 수 없습니다.
 */
data class AttendanceTimeOffsets(
    val attendanceOpenMinutesBeforeStart: Int,
    val lateAfterStartMinutes: Int,
    val absentAfterStartMinutes: Int,
) {
    init {
        val violation = violationOf(attendanceOpenMinutesBeforeStart, lateAfterStartMinutes, absentAfterStartMinutes)
        require(violation == null) { "유효하지 않은 출석 시간 정책입니다: $violation ($this)" }
    }

    fun resolveFor(sessionStart: Instant): SessionAttendanceTimes =
        SessionAttendanceTimes(
            attendanceStart = sessionStart.minus(Duration.ofMinutes(attendanceOpenMinutesBeforeStart.toLong())),
            lateStart = sessionStart.plus(Duration.ofMinutes(lateAfterStartMinutes.toLong())),
            absentStart = sessionStart.plus(Duration.ofMinutes(absentAfterStartMinutes.toLong())),
        )

    enum class Violation {
        /** 값이 [MIN_MINUTES]..[MAX_MINUTES] 범위를 벗어남 */
        OUT_OF_RANGE,

        /** 인증 시작 < 지각 시작 < 인증 마감 순서가 아님 */
        NOT_ORDERED,
    }

    companion object {
        const val MIN_MINUTES = 0

        /** 하루(24시간). 세션 시작 기준 앞뒤 하루를 넘는 정책은 오입력으로 간주합니다. */
        const val MAX_MINUTES = 1440

        const val DEFAULT_ATTENDANCE_OPEN_MINUTES_BEFORE_START = 10
        const val DEFAULT_LATE_AFTER_START_MINUTES = 15
        const val DEFAULT_ABSENT_AFTER_START_MINUTES = 30

        val DEFAULT =
            AttendanceTimeOffsets(
                attendanceOpenMinutesBeforeStart = DEFAULT_ATTENDANCE_OPEN_MINUTES_BEFORE_START,
                lateAfterStartMinutes = DEFAULT_LATE_AFTER_START_MINUTES,
                absentAfterStartMinutes = DEFAULT_ABSENT_AFTER_START_MINUTES,
            )

        fun violationOf(
            attendanceOpenMinutesBeforeStart: Int,
            lateAfterStartMinutes: Int,
            absentAfterStartMinutes: Int,
        ): Violation? {
            val range = MIN_MINUTES..MAX_MINUTES
            return when {
                attendanceOpenMinutesBeforeStart !in range ||
                    lateAfterStartMinutes !in range ||
                    absentAfterStartMinutes !in range -> Violation.OUT_OF_RANGE
                lateAfterStartMinutes >= absentAfterStartMinutes -> Violation.NOT_ORDERED
                attendanceOpenMinutesBeforeStart == 0 && lateAfterStartMinutes == 0 -> Violation.NOT_ORDERED
                else -> null
            }
        }
    }
}
