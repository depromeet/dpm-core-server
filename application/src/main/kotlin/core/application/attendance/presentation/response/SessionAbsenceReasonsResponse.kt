package core.application.attendance.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SessionAbsenceReasonsResponse(
    val reasons: List<SessionAbsenceReasonItem>,
)

data class SessionAbsenceReasonItem(
    val memberId: Long,
    val memberName: String,
    val contents: String,
    @field:Schema(description = "사유서 검토 상태", allowableValues = ["PENDING", "APPROVED", "REJECTED"])
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 []. images 와 같은 순서이며 호환을 위해 유지 */
    val imageIds: List<Long>,
    /** 첨부 이미지와 원본 파일명, 표시 순서대로. 없으면 []. 조회 URL: GET /v3/images/{imageId}, 다운로드: GET /v3/images/{imageId}/download */
    val images: List<AbsenceReasonImageInfo>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    @field:Schema(description = "심사 정정에 필요한 버전. 조회한 문자열을 해석하거나 바꾸지 않고 expectedReviewVersion에 그대로 전달")
    val reviewVersion: String,
)
