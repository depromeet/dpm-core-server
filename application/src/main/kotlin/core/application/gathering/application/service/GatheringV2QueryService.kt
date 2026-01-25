package core.application.gathering.application.service

import core.domain.gathering.port.inbound.GatheringV2QueryUseCase
import core.domain.gathering.port.outbound.GatheringPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringV2QueryService(
    val gatheringV2PersistencePort: GatheringPersistencePort,
) : GatheringV2QueryUseCase
