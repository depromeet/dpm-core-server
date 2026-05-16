package core.application.afterParty.presentation.request

import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.InviteTagEnum

data class CreateAfterPartyInviteTagRequest(
    val cohortId: CohortId,
    val authorityId: AuthorityId,
) {
    fun toDomain() =
        InviteTagEnum.from(
            cohortId = cohortId,
            authorityId = authorityId,
        )
}
