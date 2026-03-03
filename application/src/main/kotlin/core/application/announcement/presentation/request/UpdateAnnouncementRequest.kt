package core.application.announcement.presentation.request

import core.domain.announcement.enums.AnnouncementType
import java.time.LocalDateTime

data class UpdateAnnouncementRequest(
    val announcementType: AnnouncementType,
    val title: String,
    val content: String?,
    val assignment: UpdateAssignmentRequest?,
    val scheduledAt: LocalDateTime?,
    val shouldSendNotification: Boolean,
)
