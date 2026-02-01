package core.domain.announcement.port.outbound.query

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AnnouncementId
import java.time.Instant

data class AnnouncementListItemQueryModel(
    val announcementId: AnnouncementId,
    val title: String,
    val announcementType: AnnouncementType,
    val submitType: SubmitType?,
    val createdAt: Instant,
    val readMemberCount: Int?,
)
