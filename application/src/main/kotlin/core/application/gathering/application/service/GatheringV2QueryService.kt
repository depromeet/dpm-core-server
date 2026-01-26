package core.application.gathering.application.service

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.inbound.GatheringV2QueryUseCase
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringV2QueryService(
    val gatheringV2PersistencePort: GatheringV2PersistencePort,
) : GatheringV2QueryUseCase {
    override fun getAllGatherings(): List<GatheringV2> = gatheringV2PersistencePort.findAll()
}
