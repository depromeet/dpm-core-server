package core.application.attendance.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AttendanceResponse(
    @Schema(description = "출석 상태", example = "PRESENT", requiredMode = Schema.RequiredMode.REQUIRED)
    val attendanceStatus: String,
    val attendedAt: LocalDateTime,
)
