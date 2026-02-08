package core.domain.gathering.port.outbound

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member

interface GatheringV2PersistencePort {
    fun save(
        gatheringV2: GatheringV2,
        authorMember: Member,
    ): GatheringV2

    fun findById(gatheringV2Id: GatheringV2Id): GatheringV2?

    fun findAll(): List<GatheringV2>

    fun findByInviteTagFilters(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<GatheringV2>
}
