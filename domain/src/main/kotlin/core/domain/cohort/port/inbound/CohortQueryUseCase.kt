package core.domain.cohort.port.inbound

import core.domain.cohort.vo.CohortId

interface CohortQueryUseCase {
    fun getLatestCohortId(): CohortId
}
