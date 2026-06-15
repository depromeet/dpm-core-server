package core.application.attendance.presentation.response

import java.time.LocalDateTime

data class SessionAbsenceReasonsResponse(
    val reasons: List<SessionAbsenceReasonItem>,
)

data class SessionAbsenceReasonItem(
    val memberId: Long,
    val memberName: String,
    val contents: String,
    val status: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
