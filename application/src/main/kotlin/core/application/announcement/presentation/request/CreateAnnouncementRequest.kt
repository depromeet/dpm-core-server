package core.application.announcement.presentation.request

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import java.time.LocalDateTime

data class CreateAnnouncementRequest(
    val announcementType: AnnouncementType,
    val submitType: SubmitType,
    val title: String,
    val content: String?,
    val submitLink: String?,
    val startAt: LocalDateTime,
    val dueAt: LocalDateTime,
    val scheduledAt: LocalDateTime?,
    val shouldSendNotification: Boolean,
)
