package core.domain.session.vo

import java.time.Instant

data class AttendancePolicy(
    val attendanceStart: Instant,
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
