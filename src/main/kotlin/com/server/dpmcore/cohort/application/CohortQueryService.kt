package com.server.dpmcore.cohort.application

import com.server.dpmcore.cohort.application.config.CohortProperties
import com.server.dpmcore.cohort.domain.exception.CohortNotFoundException
import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.cohort.domain.port.inbound.CohortQueryUseCase
import com.server.dpmcore.cohort.domain.port.outbount.CohortPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CohortQueryService(
    private val cohortPersistencePort: CohortPersistencePort,
    private val cohortProperties: CohortProperties,
) : CohortQueryUseCase {
    override fun getLatestCohortId(): CohortId =
        cohortPersistencePort.findCohortIdByValue(cohortProperties.value)
            ?: throw CohortNotFoundException()
}
