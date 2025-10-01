package core.persistence.session.repository

import core.entity.session.SessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SessionJpaRepository : JpaRepository<SessionEntity, Long> {
    fun findFirstByDateAfterOrderByDateAsc(startOfToday: Instant): SessionEntity?

    fun findAllByCohortIdOrderByIdAsc(cohortId: Long): List<SessionEntity>
}
