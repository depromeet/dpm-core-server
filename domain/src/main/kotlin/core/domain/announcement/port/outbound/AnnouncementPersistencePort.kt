package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId

interface AnnouncementPersistencePort {
    fun save(announcement: Announcement): Announcement

    fun findAll(): List<Announcement>

    fun findAnnouncementListItems(): List<AnnouncementListItemQueryModel>

    fun findAnnouncementById(announcementId: AnnouncementId): Announcement?
}
