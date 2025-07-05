package com.server.dpmcore.session.domain.model

import java.time.Instant

<<<<<<< HEAD
class AttendancePolicy(
=======
data class AttendancePolicy(
>>>>>>> 91b27ce (feat: Session 애그리거트 도메인 모델 구현 (#19))
    val attendanceStart: Instant,
    val attendanceEnd: Instant,
    val latenessStart: Instant,
    val latenessEnd: Instant,
    val attendanceCode: String,
<<<<<<< HEAD
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttendancePolicy) return false
:
        return attendanceStart == other.attendanceStart &&
            attendanceEnd == other.attendanceEnd &&
            latenessStart == other.latenessStart &&
            latenessEnd == other.latenessEnd &&
            attendanceCode == other.attendanceCode
    }

    override fun hashCode(): Int {
        var result = attendanceStart.hashCode()
        result = 31 * result + attendanceEnd.hashCode()
        result = 31 * result + latenessStart.hashCode()
        result = 31 * result + latenessEnd.hashCode()
        result = 31 * result + attendanceCode.hashCode()
        return result
    }
}
=======
)
>>>>>>> 91b27ce (feat: Session 애그리거트 도메인 모델 구현 (#19))
