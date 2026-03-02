package core.application.gathering.presentation.request

import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.CohortId

data class CreateGatheringV2InviteTagRequest(
    val cohortId: CohortId,
    val authorityId: Long,
) {
    fun toDomain() =
        AfterPartyInviteTagEnum.from(
            cohortId = cohortId,
            authorityId = authorityId,
        )
}
