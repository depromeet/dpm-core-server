package core.persistence.afterParty.repository

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.port.outbound.AfterPartyPersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.entity.afterParty.AfterPartyEntity
import core.entity.member.MemberEntity
import org.springframework.stereotype.Repository

@Repository
class AfterPartyRepository(
    private val afterPartyJpaRepository: AfterPartyJpaRepository,
    private val afterPartyInviteTagRepository: AfterPartyInviteTagRepository,
) : AfterPartyPersistencePort {
    override fun save(
        afterParty: AfterParty,
        authorMember: Member,
    ): AfterParty {
        val authorMemberEntity = MemberEntity.from(authorMember)
        val entity = AfterPartyEntity.of(afterParty, authorMemberEntity)
        return afterPartyJpaRepository.save(entity).toDomain()
    }

    override fun update(afterParty: AfterParty): AfterParty {
        val existingEntity: AfterPartyEntity = afterPartyJpaRepository.findById(afterParty.id!!.value).get()

        val updatedEntity: AfterPartyEntity = existingEntity.updateFrom(afterParty)
        return afterPartyJpaRepository.save(updatedEntity).toDomain()
    }

    override fun findById(afterPartyId: AfterPartyId): AfterParty? =
        afterPartyJpaRepository
            .findById(afterPartyId.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findAll(): List<AfterParty> =
        afterPartyJpaRepository
            .findAllBy()
            .map { it.toDomain() }

    override fun findByInviteTagFilters(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<AfterParty> {
        val gatheringIds = afterPartyInviteTagRepository.findAfterPartyIdsByInviteTag(cohortId, authorityId)
        return afterPartyJpaRepository
            .findAllById(gatheringIds.map { it.value })
            .map { it.toDomain() }
    }
}
