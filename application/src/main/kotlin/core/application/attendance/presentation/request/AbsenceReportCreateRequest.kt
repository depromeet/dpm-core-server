package core.application.attendance.presentation.request

import core.domain.image.vo.ImageId
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 제출 요청")
data class AbsenceReportCreateRequest(
    @Schema(description = "결석 사유 (최대 50자)", example = "아파서 병원다녀옴", maxLength = 50)
    val contents: String,
    @ArraySchema(
        arraySchema =
            Schema(
                description = ABSENCE_REASON_IMAGE_IDS_DESCRIPTION,
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            ),
        schema = Schema(type = "integer", format = "int64", example = "12"),
    )
    val imageIds: List<Long?>? = null,
) {
    fun toImageIds(): List<ImageId>? = imageIds?.let(::toAbsenceReasonImageIds)
}
