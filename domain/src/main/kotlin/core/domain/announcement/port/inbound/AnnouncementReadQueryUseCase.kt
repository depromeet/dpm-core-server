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
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): Boolean

    fun getByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead>

    fun readMemberCountByAnnouncementId(announcementId: AnnouncementId): Int

    fun findByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead?
}
