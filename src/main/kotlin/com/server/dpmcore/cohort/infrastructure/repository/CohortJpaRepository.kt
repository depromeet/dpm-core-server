package com.server.dpmcore.cohort.infrastructure.repository

import com.server.dpmcore.cohort.infrastructure.entity.CohortEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CohortJpaRepository : JpaRepository<CohortEntity, Long>
