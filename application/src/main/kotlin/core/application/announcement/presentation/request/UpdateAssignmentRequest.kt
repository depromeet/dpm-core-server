package core.application.announcement.presentation.request

import core.domain.announcement.enums.SubmitType
import java.time.LocalDateTime

data class UpdateAssignmentRequest(
    val submitType: SubmitType,
    val submitLink: String?,
    val startAt: LocalDateTime?,
    val dueAt: LocalDateTime?,
)
