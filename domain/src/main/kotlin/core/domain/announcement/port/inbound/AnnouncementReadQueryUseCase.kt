package core.domain.announcement.port.inbound

import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId

interface AnnouncementReadQueryUseCase {
    fun getByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    )
}
