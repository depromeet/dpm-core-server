package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class MyAbsenceReasonResponse(
    val contents: String,
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 빈 목록. 조회 URL 은 GET /v3/images/{imageId} */
    val imageIds: List<Long>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
