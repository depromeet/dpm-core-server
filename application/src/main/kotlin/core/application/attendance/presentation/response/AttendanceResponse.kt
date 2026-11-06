package core.application.attendance.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AttendanceResponse(
    @Schema(description = "출석 상태", example = "PRESENT", requiredMode = Schema.RequiredMode.REQUIRED)
    val attendanceStatus: String,
    val attendedAt: LocalDateTime,
)

/** 출석 등록의 200 응답과 예시를 함께 제공하기 위한 OpenAPI 모델. */
internal data class AttendanceRecordSuccessResponse(
    val status: String,
    val message: String,
    val code: String,
    val data: AttendanceResponse,
)
