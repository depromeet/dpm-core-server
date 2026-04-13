package core.persistence.announcement.repository

import core.entity.announcement.AssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface AssignmentJpaRepository : JpaRepository<AssignmentEntity, Long> {
    fun findByDueAtBetweenAndDeletedAtIsNull(
        start: Instant,
        end: Instant,
    ): List<AssignmentEntity>
}
