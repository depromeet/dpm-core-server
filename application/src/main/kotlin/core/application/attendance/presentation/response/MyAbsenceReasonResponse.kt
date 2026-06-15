package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class MyAbsenceReasonResponse(
    val contents: String,
    val status: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
