package core.domain.attendance.vo

import core.domain.session.vo.SessionAttendanceTimes
import java.time.Duration
import java.time.Instant

/** 세션 시작(T) 기준 기본 출석 시간(분): 인증 시작 T-open, 지각 시작 T+late, 인증 마감 T+absent. */
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
        OUT_OF_RANGE,
        NOT_ORDERED,
    }

    companion object {
        const val MIN_MINUTES = 0

        /** 하루를 넘는 값은 오입력으로 본다. */
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
