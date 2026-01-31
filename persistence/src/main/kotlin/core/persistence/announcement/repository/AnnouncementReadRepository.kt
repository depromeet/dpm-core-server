package core.persistence.announcement.repository

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.outbound.AnnouncementReadPersistencePort
import core.entity.announcement.AnnouncementReadEntity
import org.springframework.stereotype.Repository

@Repository
class AnnouncementReadRepository(
    val announcementReadJpaRepository: AnnouncementReadJpaRepository,
) : AnnouncementReadPersistencePort {
    override fun save(announcementRead: AnnouncementRead): AnnouncementRead =
        announcementReadJpaRepository.save(AnnouncementReadEntity.from(announcementRead)).toDomain()

    override fun findByMemberIdAndAnnouncementId(
        memberId: Long,
        announcementId: Long,
    ): AnnouncementRead? {
        TODO("Not yet implemented")
    }
}
