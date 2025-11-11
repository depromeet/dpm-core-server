package core.domain.session.extension

import core.domain.session.vo.AttendancePolicy
import java.time.Instant

fun AttendancePolicy.hasChangedComparedTo(
    attendanceStart: Instant,
    lateStart: Instant,
    absentStart: Instant,
): Boolean {
    return this.attendanceStart != attendanceStart ||
        this.lateStart != lateStart ||
        this.absentStart != absentStart
}
