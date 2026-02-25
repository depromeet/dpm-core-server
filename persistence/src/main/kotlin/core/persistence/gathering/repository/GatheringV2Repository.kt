package core.persistence.gathering.repository

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.entity.gathering.GatheringV2Entity
import core.entity.member.MemberEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringV2Repository(
    private val gatheringV2JpaRepository: GatheringV2JpaRepository,
    private val gatheringV2InviteTagRepository: GatheringV2InviteTagRepository,
) : GatheringV2PersistencePort {
    override fun save(
        gatheringV2: GatheringV2,
        authorMember: Member,
    ): GatheringV2 {
        val authorMemberEntity = MemberEntity.from(authorMember)
        val entity = GatheringV2Entity.of(gatheringV2, authorMemberEntity)
        return gatheringV2JpaRepository.save(entity).toDomain()
    }

    override fun update(gatheringV2: GatheringV2): GatheringV2 {
        val existingEntity: GatheringV2Entity = gatheringV2JpaRepository.findById(gatheringV2.id!!.value).get()

        val updatedEntity: GatheringV2Entity = existingEntity.updateFrom(gatheringV2)
        return gatheringV2JpaRepository.save(updatedEntity).toDomain()
    }

    override fun findById(gatheringV2Id: GatheringV2Id): GatheringV2? =
        gatheringV2JpaRepository.findById(gatheringV2Id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findAll(): List<GatheringV2> =
        gatheringV2JpaRepository.findAllBy()
            .map { it.toDomain() }

    override fun findByInviteTagFilters(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<GatheringV2> {
        val gatheringIds = gatheringV2InviteTagRepository.findGatheringIdsByInviteTag(cohortId, authorityId)
        return gatheringV2JpaRepository.findAllById(gatheringIds.map { it.value })
            .map { it.toDomain() }
    }
}
