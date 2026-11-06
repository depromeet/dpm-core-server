package core.domain.attendance.port.outbound.query

import core.domain.attendance.aggregate.Attendance
import core.domain.session.aggregate.Session
import java.time.Instant

data class MyDetailAttendanceQueryModel(
    val attendance: Attendance,
    val sessionAbsentStart: Instant,
    val sessionDeleted: Boolean,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: Instant,
    val sessionPlace: String,
) {
    val attendedAt: Instant? get() = attendance.attendedAt.takeIf { attendance.updatedAt == null }

    fun isLateParticipationAvailableAt(now: Instant): Boolean =
        !sessionDeleted &&
            attendance.canRecordAttendance() &&
            Session.isLateParticipationOpenAt(sessionDate, sessionAbsentStart, now)
}
