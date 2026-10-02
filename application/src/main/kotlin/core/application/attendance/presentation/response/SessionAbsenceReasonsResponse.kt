package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class SessionAbsenceReasonsResponse(
    val reasons: List<SessionAbsenceReasonItem>,
)

data class SessionAbsenceReasonItem(
    val memberId: Long,
    val memberName: String,
    val contents: String,
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 빈 목록. 원본은 GET /v2/sessions/{sessionId}/absence-reasons/{memberId}/images/{imageId} */
    val imageIds: List<Long>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
