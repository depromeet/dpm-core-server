package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class MyAbsenceReasonResponse(
    val contents: String,
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 빈 목록. images 와 같은 순서이며 호환을 위해 유지 */
    val imageIds: List<Long>,
    /** 첨부 이미지와 원본 파일명, 표시 순서대로. 없으면 빈 목록. 조회 URL 은 GET /v3/images/{imageId} */
    val images: List<AbsenceReasonImageInfo>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
