package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class DetailAttendancesBySessionResponse(
    val member: DetailMember,
    val session: DetailSession,
    val attendance: DetailAttendance,
) {
    data class DetailMember(
        val id: Long,
        val name: String,
        val teamNumber: Int,
        val part: String,
        val attendanceStatus: String,
    )

    data class DetailSession(
        val id: Long,
        val week: Int,
        val eventName: String,
        val date: LocalDateTime,
    )

    data class DetailAttendance(
        val status: String,
        val attendedAt: LocalDateTime?,
        val updatedAt: LocalDateTime?,
    )
}
