package com.server.dpmcore.cohort.domain.port.outbound

import com.server.dpmcore.cohort.domain.model.Cohort

interface CohortPersistencePort {
    fun findByValue(value: String): Cohort
}
