package core.domain.afterParty.port.outbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.CohortId

interface AfterPartyInviteTagPersistencePort {
    fun save(
        afterPartyInviteTag: AfterPartyInviteTag,
        afterParty: AfterParty,
    )

    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag>

    fun findAfterPartyIdsByInviteTag(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<AfterPartyId>

    fun findAllDistinct(): List<AfterPartyInviteTag>

    fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag>

    fun deleteByAfterPartyId(afterPartyId: AfterPartyId)
}
