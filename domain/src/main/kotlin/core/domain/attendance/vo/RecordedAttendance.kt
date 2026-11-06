package core.domain.attendance.vo

import core.domain.attendance.enums.AttendanceStatus
import java.time.Instant

data class RecordedAttendance(
    val status: AttendanceStatus,
    val attendedAt: Instant,
)
