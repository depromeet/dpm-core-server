package core.application.gathering.presentation.response

import core.domain.cohort.vo.CohortId
import core.domain.gathering.enums.GatheringV2InviteTag

data class GatheringV2InviteTagListResponse(
    val inviteTags: List<GatheringV2InviteTagNameResponse> =
        listOf(
            GatheringV2InviteTagNameResponse.from(GatheringV2InviteTag.from(CohortId(1L), 1L)),
            GatheringV2InviteTagNameResponse.from(GatheringV2InviteTag.from(CohortId(1L), 2L)),
        ),
)
