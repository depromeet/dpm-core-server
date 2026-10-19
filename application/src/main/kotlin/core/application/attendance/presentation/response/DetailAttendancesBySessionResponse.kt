package core.application.attendance.presentation.response

import core.domain.team.vo.TeamNumber
import java.time.LocalDateTime

data class DetailAttendancesBySessionResponse(
    val member: DetailMember,
    val session: DetailSession,
    val attendance: DetailAttendance,
) {
    data class DetailMember(
        val id: Long,
        val name: String,
        val teamNumber: TeamNumber,
        val isAdmin: Boolean,
        val part: String?,
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
        /** 실제 출석 인증 시각(Asia/Seoul). 인증하지 않았거나 운영진이 상태를 바꿨으면 null */
        val attendedAt: LocalDateTime?,
        /** 운영진 변경 시각(Asia/Seoul). 없으면 null */
        val updatedAt: LocalDateTime?,
    )
}
