package core.persistence.gathering.repository

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.outbound.GatheringV2InviteTagPersistencePort
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.entity.gathering.GatheringV2Entity
import core.entity.member.MemberEntity
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class GatheringV2Repository(
    val gatheringV2JpaRepository: GatheringV2JpaRepository,
    val gatheringV2InviteTagPersistencePort: GatheringV2InviteTagPersistencePort,
) : GatheringV2PersistencePort {
    override fun save(
        gatheringV2: GatheringV2,
        authorMember: Member,
    ): GatheringV2 =
        gatheringV2JpaRepository.save(
            GatheringV2Entity.of(
                gatheringV2,
                MemberEntity.from(authorMember),
            ),
        ).toDomain()

    override fun findById(gatheringV2Id: GatheringV2Id): GatheringV2? =
        gatheringV2JpaRepository.findById(
            gatheringV2Id.value,
        ).getOrNull()?.toDomain()

    override fun findAll(): List<GatheringV2> = gatheringV2JpaRepository.findAll().map { it.toDomain() }

    override fun findByInviteTagFilters(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<GatheringV2> {
        if (cohortId == null && authorityId == null) {
            return findAll()
        }

        val gatheringIds: List<GatheringV2Id> = gatheringV2InviteTagPersistencePort.findGatheringIdsByInviteTag(
            cohortId = cohortId,
            authorityId = authorityId
        )

        if (gatheringIds.isEmpty()) {
            return emptyList()
        }

        val ids = gatheringIds.map { it.value }
        return gatheringV2JpaRepository.findAllById(ids).map { it.toDomain() }
    }
}
