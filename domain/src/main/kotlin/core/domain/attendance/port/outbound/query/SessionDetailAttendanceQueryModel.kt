package core.domain.attendance.port.outbound.query

import core.domain.team.vo.TeamNumber
import java.time.Instant

data class SessionDetailAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val summary: AttendanceSummaryQueryModel,
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: Instant,
    val attendanceStatus: String,
    /** 실제 출석 인증 시각. 인증하지 않았거나 운영진이 상태를 바꿨으면(updatedAt 있음) null */
    val attendedAt: Instant?,
    /** 운영진 변경 시각. 운영진만 기록한다 */
    val updatedAt: Instant?,
)
