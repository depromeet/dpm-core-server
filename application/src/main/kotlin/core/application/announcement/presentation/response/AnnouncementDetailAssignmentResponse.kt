package core.application.announcement.presentation.response

import core.domain.announcement.enums.SubmitType
import java.time.LocalDateTime

data class AnnouncementDetailAssignmentResponse(
    val submitType: SubmitType,
    val startAt: LocalDateTime?,
    val dueAt: LocalDateTime?,
    val submitLink: String?,
) {
    companion object {
        fun of(
            submitType: SubmitType,
            startAt: LocalDateTime?,
            dueAt: LocalDateTime?,
            submitLink: String?,
        ): AnnouncementDetailAssignmentResponse =
            AnnouncementDetailAssignmentResponse(
                submitType = submitType,
                startAt = startAt,
                dueAt = dueAt,
                submitLink = submitLink,
            )
    }
}
