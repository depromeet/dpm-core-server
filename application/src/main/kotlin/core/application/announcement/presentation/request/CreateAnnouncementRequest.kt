package core.application.announcement.presentation.request

import core.domain.announcement.enums.AnnouncementType
import java.time.LocalDateTime

data class CreateAnnouncementRequest(
    val announcementType: AnnouncementType,
    val title: String,
    val content: String?,
    val createAssignmentRequest: CreateAssignmentRequest?,
    val scheduledAt: LocalDateTime?,
    val shouldSendNotification: Boolean,
)
