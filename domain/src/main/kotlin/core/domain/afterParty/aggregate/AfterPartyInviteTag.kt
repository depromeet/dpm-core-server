package core.domain.afterParty.aggregate

import core.domain.afterParty.vo.AfterPartyId
import core.domain.afterParty.vo.AfterPartyInviteTagId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import java.time.Instant

class AfterPartyInviteTag(
    val id: AfterPartyInviteTagId? = null,
    val afterPartyId: AfterPartyId,
    val cohortId: CohortId,
    val authorityId: AuthorityId,
    val tagName: String,
    val createdAt: Instant? = null,
) {
    companion object {
        fun create(
            afterPartyId: AfterPartyId,
            cohortId: CohortId,
            authorityId: AuthorityId,
            tagName: String,
        ): AfterPartyInviteTag =
            AfterPartyInviteTag(
                afterPartyId = afterPartyId,
                cohortId = cohortId,
                authorityId = authorityId,
                tagName = tagName,
                createdAt = Instant.now(),
            )
    }
}
