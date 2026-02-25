package core.application.gathering.presentation.response

import core.domain.cohort.vo.CohortId
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.gathering.aggregate.GatheringV2InviteTag as GatheringV2InviteTagAggregate

data class GatheringV2InviteTagNameResponse(
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
) {
    companion object {
        fun from(gatheringV2InviteTag: GatheringV2InviteTag): GatheringV2InviteTagNameResponse =
            GatheringV2InviteTagNameResponse(
                cohortId = gatheringV2InviteTag.cohortId,
                authorityId = gatheringV2InviteTag.authorityId,
                tagName = gatheringV2InviteTag.tagName,
            )

        fun from(gatheringV2InviteTag: GatheringV2InviteTagAggregate): GatheringV2InviteTagNameResponse =
            GatheringV2InviteTagNameResponse(
                cohortId = gatheringV2InviteTag.cohortId,
                authorityId = gatheringV2InviteTag.authorityId,
                tagName = gatheringV2InviteTag.tagName,
            )
    }
}
