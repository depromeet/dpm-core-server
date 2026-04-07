package core.domain.afterParty.port.outbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member

interface AfterPartyPersistencePort {
    fun save(
        afterParty: AfterParty,
        authorMember: Member,
    ): AfterParty

    fun update(afterParty: AfterParty): AfterParty

    fun findById(afterPartyId: AfterPartyId): AfterParty?

    fun findAll(): List<AfterParty>

    fun findByInviteTagFilters(
        cohortId: CohortId?,
        authorityId: AuthorityId?,
    ): List<AfterParty>
}
