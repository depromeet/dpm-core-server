package core.application.gathering.presentation.request

import core.domain.cohort.vo.CohortId
import core.domain.gathering.enums.GatheringV2InviteTag

data class CreateGatheringV2InviteTagRequest(
    val cohortId: CohortId,
    val authorityId: Long,
) {
    fun toDomain() =
        GatheringV2InviteTag.from(
            cohortId = cohortId,
            authorityId = authorityId,
        )
}
