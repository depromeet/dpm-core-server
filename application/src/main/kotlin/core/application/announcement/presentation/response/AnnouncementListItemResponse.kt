package core.application.announcement.presentation.response

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AnnouncementId
import java.time.LocalDateTime

data class AnnouncementListItemResponse(
    val announcementId: AnnouncementId,
    val title: String,
    val announcementType: AnnouncementType,
    val assignmentType: SubmitType,
    val createdAt: LocalDateTime,
    val readMemberCount: Int,
) {
}
