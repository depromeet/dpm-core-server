package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId

interface AnnouncementReadPersistencePort {
    fun save(announcementRead: AnnouncementRead): AnnouncementRead

    fun saveAll(announcementReads: List<AnnouncementRead>)

    fun findByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead?

    fun existsByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): Boolean

    fun findAllByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead>

    fun countByAnnouncementIdAndReadAtIsNotNull(announcementId: AnnouncementId): Int

    fun findByAnnouncementIdAndReadAtIsNull(announcementId: AnnouncementId): List<AnnouncementRead>
}
