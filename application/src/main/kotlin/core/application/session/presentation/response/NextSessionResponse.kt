package core.application.session.presentation.response

import java.time.LocalDateTime

data class NextSessionResponse(
    val id: Long,
    val week: Int,
    val name: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
    val attendanceCode: String,
    /** 운영진(ORGANIZER/CORE/MASTER)은 false. 디퍼 출석 배너·인증 CTA 노출에 쓴다. */
    val canRecordAttendance: Boolean = true,
)
