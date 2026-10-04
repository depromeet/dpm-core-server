package core.domain.session.vo

import java.time.Instant

/** 세션에 확정된 출석 시각. 각 경계 시각은 그 구간에 포함된다(attendanceStart 부터 출석, lateStart 부터 지각). */
data class SessionAttendanceTimes(
    val attendanceStart: Instant,
    val lateStart: Instant,
    val absentStart: Instant,
) {
    init {
        require(isOrdered(attendanceStart, lateStart, absentStart)) {
            "출석 시각은 attendanceStart < lateStart < absentStart 순서여야 합니다. " +
                "attendanceStart=$attendanceStart, lateStart=$lateStart, absentStart=$absentStart"
        }
    }

    companion object {
        fun isOrdered(
            attendanceStart: Instant,
            lateStart: Instant,
            absentStart: Instant,
        ): Boolean = attendanceStart.isBefore(lateStart) && lateStart.isBefore(absentStart)
    }
}
