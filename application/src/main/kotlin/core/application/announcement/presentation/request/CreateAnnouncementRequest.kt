package core.application.announcement.presentation.request

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import java.time.Instant

data class CreateAnnouncementRequest(
    val announcementType: AnnouncementType,
    val submitType: SubmitType,
    val title: String,
    val content: String?,
    val submitLink: String?,
    val startAt: Instant,
    val dueAt: Instant,
    val scheduledAt: Instant?,
    val shouldSendNotification: Boolean,
)
