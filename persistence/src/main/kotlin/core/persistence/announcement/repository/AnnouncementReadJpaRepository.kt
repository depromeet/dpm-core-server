package core.persistence.announcement.repository

import core.entity.announcement.AnnouncementReadEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementReadJpaRepository : JpaRepository<AnnouncementReadEntity, Long>
