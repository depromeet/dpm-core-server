package core.domain.session.vo

import java.time.Instant

/**
 * 세션 하나에 확정된 출석 인증 시각들입니다.
 *
 * 경계는 모두 해당 시각을 포함합니다.
 * - [attendanceStart] 부터 인증 가능 (PRESENT)
 * - [lateStart] 부터 지각 (LATE)
 * - [absentStart] 부터 인증 마감 (자동 결석 대상)
 *
 * 세 시각은 반드시 attendanceStart < lateStart < absentStart 순서여야 합니다.
 */
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
