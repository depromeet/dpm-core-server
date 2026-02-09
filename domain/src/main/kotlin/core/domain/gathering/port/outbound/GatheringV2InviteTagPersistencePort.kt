package core.domain.gathering.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.vo.GatheringV2Id

interface GatheringV2InviteTagPersistencePort {
    fun save(
        gatheringV2InviteTag: GatheringV2InviteTag,
        gatheringV2: GatheringV2,
    )

    fun findByGatheringId(gatheringId: GatheringV2Id): List<GatheringV2InviteTag>

    fun findGatheringIdsByInviteTag(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<GatheringV2Id>

    fun findAllDistinct(): List<GatheringV2InviteTag>

    fun deleteByGatheringId(gatheringId: GatheringV2Id)
}
