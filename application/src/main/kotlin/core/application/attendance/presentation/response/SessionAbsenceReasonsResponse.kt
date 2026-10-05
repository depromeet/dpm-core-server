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
    /** 첨부 이미지 id, 표시 순서대로. 없으면 []. 조회 URL: GET /v3/images/{imageId}, 다운로드: GET /v3/images/{imageId}/download */
    val imageIds: List<Long>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
