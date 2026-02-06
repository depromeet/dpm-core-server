package core.persistence.announcement.repository

import core.entity.announcement.AnnouncementEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementJpaRepository : JpaRepository<AnnouncementEntity, Long>
