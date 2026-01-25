package core.persistence.gathering.repository

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.entity.gathering.GatheringV2Entity
import org.springframework.stereotype.Repository

@Repository
class GatheringV2Repository(
    val gatheringV2JpaRepository: GatheringV2JpaRepository,
) : GatheringV2PersistencePort {
    override fun save(gatheringV2: GatheringV2): GatheringV2 =
        gatheringV2JpaRepository.save(GatheringV2Entity.from(gatheringV2)).toDomain()

    override fun findById(gatheringV2Id: GatheringV2Id): List<GatheringV2> {
        TODO("Not yet implemented")
    }
}
