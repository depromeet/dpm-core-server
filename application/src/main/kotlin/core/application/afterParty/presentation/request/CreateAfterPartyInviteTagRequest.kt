package core.application.afterParty.presentation.request

import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.CohortId

data class CreateAfterPartyInviteTagRequest(
    val cohortId: CohortId,
    val authorityId: Long,
) {
    fun toDomain() =
        AfterPartyInviteTagEnum.from(
            cohortId = cohortId,
            authorityId = authorityId,
        )
}
