package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId

interface AnnouncementReadCommandUseCase {
    fun markAsRead(
        memberId: MemberId,
        announcementId: AnnouncementId,
    ): AnnouncementRead
}
