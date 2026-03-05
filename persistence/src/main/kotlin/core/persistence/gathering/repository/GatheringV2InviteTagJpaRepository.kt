package core.persistence.gathering.repository

import core.entity.gathering.GatheringV2InviteTagEntity
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("FunctionName")
interface GatheringV2InviteTagJpaRepository : JpaRepository<GatheringV2InviteTagEntity, Long> {
    fun findByGathering_Id(gatheringId: Long): List<GatheringV2InviteTagEntity>

    fun deleteByGathering_Id(gatheringId: Long)
}
