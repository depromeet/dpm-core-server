package core.domain.announcement.aggregate

import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AnnouncementReadId
import core.domain.member.vo.MemberId
import java.time.Instant

class AnnouncementRead(
    val id: AnnouncementReadId? = null,
    val announcementId: AnnouncementId,
    val memberId: MemberId,
    val readAt: Instant,
)
