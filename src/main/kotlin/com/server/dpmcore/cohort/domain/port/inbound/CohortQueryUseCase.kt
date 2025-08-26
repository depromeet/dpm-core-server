package com.server.dpmcore.cohort.domain.port.inbound

import com.server.dpmcore.cohort.domain.model.CohortId

interface CohortQueryUseCase {
    fun getLatestCohortId(): CohortId
}
