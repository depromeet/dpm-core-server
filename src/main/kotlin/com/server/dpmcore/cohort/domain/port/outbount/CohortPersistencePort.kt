package com.server.dpmcore.cohort.domain.port.outbount

import com.server.dpmcore.cohort.domain.model.CohortId

interface CohortPersistencePort {
    fun findCohortIdByValue(value: String): CohortId?
}
