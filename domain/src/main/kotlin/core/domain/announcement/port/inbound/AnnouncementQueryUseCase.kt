package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.vo.AnnouncementId

interface AnnouncementQueryUseCase {
    fun getAnnouncementById(announcementId: AnnouncementId): Announcement
}
