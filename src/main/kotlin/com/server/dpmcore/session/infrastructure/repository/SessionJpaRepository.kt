package com.server.dpmcore.session.infrastructure.repository

import com.server.dpmcore.session.infrastructure.entity.SessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SessionJpaRepository : JpaRepository<SessionEntity, Long> {
    fun findFirstByDateAfterOrderByDateAsc(startOfToday: Instant): SessionEntity?

    fun findAllByCohortIdOrderByIdAsc(cohortId: Long): List<SessionEntity>
}
