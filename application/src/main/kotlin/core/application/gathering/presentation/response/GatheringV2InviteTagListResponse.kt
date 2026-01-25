package core.application.gathering.presentation.response

import core.domain.cohort.vo.CohortId
import core.domain.gathering.enums.GatheringV2InviteTagName

data class GatheringV2InviteTagListResponse(
    val inviteTags: List<GatheringV2InviteTag> =
        listOf(
            GatheringV2InviteTag.from(GatheringV2InviteTagName.from(CohortId(1L), 1L)),
            GatheringV2InviteTag.from(GatheringV2InviteTagName.from(CohortId(1L), 2L)),
        ),
)

data class GatheringV2InviteTag(
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
) {
    companion object {
        fun from(gatheringV2InviteTagName: GatheringV2InviteTagName): GatheringV2InviteTag =
            GatheringV2InviteTag(
                cohortId = gatheringV2InviteTagName.cohortId,
                authorityId = gatheringV2InviteTagName.authorityId,
                tagName = gatheringV2InviteTagName.tagName,
            )
    }
}
