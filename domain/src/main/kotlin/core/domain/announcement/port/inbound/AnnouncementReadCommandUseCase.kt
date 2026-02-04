package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId

interface AnnouncementReadCommandUseCase {
    fun markAsRead(announcementRead: AnnouncementRead): AnnouncementRead

    fun initializeForMembers(
        announcementId: AnnouncementId,
        memberIds: List<MemberId>,
    )
}
