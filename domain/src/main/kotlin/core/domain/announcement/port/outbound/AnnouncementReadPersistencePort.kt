package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.AnnouncementRead

interface AnnouncementReadPersistencePort {
    fun save(announcementRead: AnnouncementRead): AnnouncementRead

    fun findByMemberIdAndAnnouncementId(
        memberId: Long,
        announcementId: Long,
    ): AnnouncementRead?
}
