package core.persistence.announcement.repository

import core.entity.announcement.AssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentJpaRepository : JpaRepository<AssignmentEntity, Long>
