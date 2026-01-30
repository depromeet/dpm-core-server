package core.persistence.announcement.repository

import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.port.outbound.AnnouncementAssignmentPersistencePort
import core.entity.announcement.AnnouncementAssignmentEntity
import org.springframework.stereotype.Repository

@Repository
class AnnouncementAssignmentRepository(
    val announcementAssignmentJpaRepository: AnnouncementAssignmentJpaRepository,
) : AnnouncementAssignmentPersistencePort {
    override fun save(announcementAssignment: AnnouncementAssignment): AnnouncementAssignment =
        announcementAssignmentJpaRepository.save(AnnouncementAssignmentEntity.from(announcementAssignment)).toDomain()
}
