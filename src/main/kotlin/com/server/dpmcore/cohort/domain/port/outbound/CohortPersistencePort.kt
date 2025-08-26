package com.server.dpmcore.cohort.domain.port.outbound

import com.server.dpmcore.cohort.domain.model.CohortId

interface CohortPersistencePort {
    fun findCohortIdByValue(value: String): CohortId?
}
