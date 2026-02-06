package core.persistence.announcement.repository

import core.entity.announcement.AnnouncementAssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementAssignmentJpaRepository : JpaRepository<AnnouncementAssignmentEntity, Long>
