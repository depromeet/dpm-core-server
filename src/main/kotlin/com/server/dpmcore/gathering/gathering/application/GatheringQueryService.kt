package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringQueryService(
    private val gatheringPersistencePort: GatheringPersistencePort,
) : GatheringQueryUseCase {
    fun findById(gatheringId: Long) =
        gatheringPersistencePort
            .findById(gatheringId)

    override fun getAllGatheringsByGatheringIds(gatheringIds: List<GatheringId>): List<Gathering> =
        gatheringPersistencePort.findAllByGatheringIds(gatheringIds)
}
