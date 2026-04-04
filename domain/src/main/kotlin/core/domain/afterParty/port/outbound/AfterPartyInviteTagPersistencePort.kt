package core.domain.afterParty.port.outbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId

interface AfterPartyInviteTagPersistencePort {
    fun save(
        afterPartyInviteTag: AfterPartyInviteTag,
        afterParty: AfterParty,
    )

    fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag>

    fun findAfterPartyIdsByInviteTag(
        cohortId: CohortId?,
        authorityId: AuthorityId?,
    ): List<AfterPartyId>

    @Deprecated("최초 1회 회식 생성 태그에서 오류가 터지므로 Enum으로 관리 책임 이관")
    fun findAllDistinct(): List<AfterPartyInviteTag>

    fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag>

    fun deleteByAfterPartyId(afterPartyId: AfterPartyId)
}
