package core.application.attendance.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 검토(승인/반려) 요청")
data class AbsenceReasonReviewRequest(
    @Schema(description = "승인 여부 (true: 승인, false: 반려)", example = "true")
    val approved: Boolean,
)
