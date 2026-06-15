package core.application.attendance.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 수정 요청")
data class AbsenceReportUpdateRequest(
    @Schema(description = "결석 사유 (최대 50자)", example = "갑자기 일이 생겨 불참", maxLength = 50)
    val contents: String,
)
