package core.application.attendance.presentation.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import core.domain.attendance.enums.AttendanceStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결석 사유서 검토(승인/반려) 요청")
data class AbsenceReasonReviewRequest(
    @Schema(description = "승인 여부 (true: 승인, false: 반려)", example = "true")
    @param:JsonProperty(required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val approved: Boolean,
    @field:Schema(description = "정정할 사유서를 조회할 때 받은 reviewVersion. 이미 심사한 사유서를 정정할 때 필수")
    val expectedReviewVersion: String? = null,
    @field:Schema(
        description = "승인에서 반려로 정정할 때 지정할 출석 상태. PENDING은 코드 인증 재개를 요청",
        allowableValues = ["ABSENT", "PRESENT", "LATE", "PENDING"],
    )
    val finalAttendanceStatus: AttendanceStatus? = null,
)
