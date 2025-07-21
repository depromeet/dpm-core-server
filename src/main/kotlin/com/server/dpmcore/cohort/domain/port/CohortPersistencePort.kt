package com.server.dpmcore.cohort.domain.port

import com.server.dpmcore.cohort.domain.model.CohortId

interface CohortPersistencePort {
    fun findLatestCohortId(): CohortId?
}
