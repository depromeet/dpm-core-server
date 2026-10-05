package core.domain.attendance.port.outbound.query

import java.time.Instant

data class MemberSessionAttendanceQueryModel(
    val sessionId: Long,
    val sessionWeek: Int,
    val sessionEventName: String,
    val sessionDate: Instant,
    val sessionIsOnline: Boolean,
    /** 저장된 세션 장소명. 온라인 세션이면 비어 있을 수 있다 */
    val sessionPlace: String,
    val sessionAttendanceStatus: String,
    /** 실제 출석 인증 시각. 인증하지 않았으면 null */
    val attendedAt: Instant?,
    /** 이 세션에 제출한 결석 사유서. 없으면 null */
    val absenceReason: AbsenceReason?,
) {
    data class AbsenceReason(
        val id: Long,
        val contents: String,
        val status: String,
        /** 첨부 이미지, 표시 순서대로 */
        val images: List<Image> = emptyList(),
    )

    data class Image(
        val imageId: Long,
        /** 업로드할 때 받은 원본 파일명. 없으면 null */
        val fileName: String?,
    )
}
