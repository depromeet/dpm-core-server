package core.domain.cohort.port.inbound

import core.domain.cohort.vo.CohortId

interface CohortQueryUseCase {
    fun getActiveCohortId(): CohortId

    fun getActiveCohortValue(): String

    /** 활성 기수가 없으면 null. getActiveCohortId()와 달리 max-value 폴백을 하지 않는다. */
    fun findActiveCohortId(): CohortId?

    fun getLatestCohortId(): CohortId = getActiveCohortId()

    fun getLatestCohortValue(): String = getActiveCohortValue()
}
