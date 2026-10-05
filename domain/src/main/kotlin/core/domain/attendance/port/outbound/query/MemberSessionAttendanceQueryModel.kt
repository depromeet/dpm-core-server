package core.domain.attendance.port.outbound.query

import java.time.Instant

data class MemberSessionAttendanceQueryModel(
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: Instant,
    val sessionIsOnline: Boolean,
    val sessionAttendanceStatus: String,
    /** 이 세션에 제출한 결석 사유서. 없으면 null */
    val absenceReason: AbsenceReason?,
) {
    data class AbsenceReason(
        val id: Long,
        val contents: String,
        val status: String,
        /** 첨부 이미지 id, 표시 순서대로 */
        val imageIds: List<Long> = emptyList(),
    )
}
