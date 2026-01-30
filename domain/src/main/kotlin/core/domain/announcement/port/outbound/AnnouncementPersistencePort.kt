package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.Announcement

interface AnnouncementPersistencePort {
    fun save(announcement: Announcement): Announcement
}
