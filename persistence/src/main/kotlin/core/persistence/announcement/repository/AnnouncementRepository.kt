package core.persistence.announcement.repository

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.entity.announcement.AnnouncementEntity
import org.springframework.stereotype.Repository

@Repository
class AnnouncementRepository(
    val announcementJpaRepository: AnnouncementJpaRepository,
) : AnnouncementPersistencePort {
    override fun save(announcement: Announcement): Announcement =
        announcementJpaRepository.save(AnnouncementEntity.from(announcement)).toDomain()
}
