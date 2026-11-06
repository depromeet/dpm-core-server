package core.application.attendance.presentation.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 검토(승인/반려) 요청")
data class AbsenceReasonReviewRequest(
    @Schema(description = "승인 여부 (true: 승인, false: 반려)", example = "true")
    @param:JsonProperty(required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val approved: Boolean,
)
