package core.persistence.session.repository

import core.entity.session.SessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SessionJpaRepository : JpaRepository<SessionEntity, Long> {
    fun findFirstByDateAfterAndDeletedAtIsNullOrderByDateAsc(startOfToday: Instant): SessionEntity?

    fun findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId: Long): List<SessionEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): SessionEntity?
}
