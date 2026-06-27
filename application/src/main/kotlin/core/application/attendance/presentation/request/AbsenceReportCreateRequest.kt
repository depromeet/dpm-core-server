package core.application.attendance.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 제출 요청")
data class AbsenceReportCreateRequest(
    @Schema(description = "결석 사유 (최대 50자)", example = "아파서 병원다녀옴", maxLength = 50)
    val contents: String,
)
