package core.domain.session.vo

import java.time.Instant

/**
 * 세션의 출석 정책을 나타내는 값 객체입니다.
 *
 * @property attendanceStart 출석 시작 시간
 * @property lateStart 지각 시작 시간(정상 출석 시간 종료)
 * @property absentStart 결석 시작 시간(지각 시간 종료)
 * @property attendanceCode 출석 코드
 *
 * @author LeeHanEum
 * @since 2025.11.10
 */
data class AttendancePolicy(
    val attendanceStart: Instant,
    val lateStart: Instant,
    val absentStart: Instant,

    /**
     * 출석 코드를 나타냅니다.
     *
     * 출석 코드는 숫자 연산을 요구하지 않기 때문에 `String` 타입을 사용합니다.
     *
     * 또한, 향후 영문자 등 다양한 형식의 출석 코드로 확장될 가능성을 고려하여 문자열 기반으로 설계되었습니다.
     *
     */
    val attendanceCode: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttendancePolicy) return false

        return attendanceStart == other.attendanceStart &&
            attendanceCode == other.attendanceCode
    }

    override fun hashCode(): Int {
        var result = attendanceStart.hashCode()
        result = 31 * result + attendanceCode.hashCode()
        return result
    }

    override fun toString(): String =
        "AttendancePolicy(" +
            "attendanceStart=$attendanceStart, " +
            "attendanceCode='$attendanceCode" +
            "')"
}
