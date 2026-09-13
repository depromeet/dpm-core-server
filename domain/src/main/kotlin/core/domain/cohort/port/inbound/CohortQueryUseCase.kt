package core.domain.cohort.port.inbound

import core.domain.cohort.vo.CohortId

interface CohortQueryUseCase {
    fun getActiveCohortId(): CohortId

    fun getActiveCohortValue(): String

    fun getLatestCohortId(): CohortId = getActiveCohortId()

    fun getLatestCohortValue(): String = getActiveCohortValue()
}
