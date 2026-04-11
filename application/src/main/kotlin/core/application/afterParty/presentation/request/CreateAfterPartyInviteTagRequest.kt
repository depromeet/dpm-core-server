package core.application.afterParty.presentation.request

import core.application.afterParty.application.exception.InviteTagNotFoundException
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId

data class CreateAfterPartyInviteTagRequest(
    val cohortId: CohortId,
    val authorityId: AuthorityId,
) {
    fun toDomain() =
        AfterPartyInviteTagEnum.from(
            cohortId = cohortId,
            authorityId = authorityId,
        ) ?: throw InviteTagNotFoundException(cohortId, authorityId)
}
