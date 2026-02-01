package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId

interface AnnouncementReadQueryUseCase {
    fun getByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead

    fun existsByAnnouncementIdAndMemberId(
        memberId: MemberId,
        announcementId: AnnouncementId,
    ): Boolean

    fun getByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead>

    fun countByAnnouncementId(announcementId: AnnouncementId): Int
}
