package core.application.gathering.presentation.response

import core.domain.cohort.vo.CohortId
import core.domain.gathering.enums.GatheringV2InviteTagName

data class GatheringV2InviteTagListResponse(
    val inviteeTags: List<GatheringV2InviteTagName> =
        listOf(
            GatheringV2InviteTagName.from(CohortId(1L), 1L),
            GatheringV2InviteTagName.from(CohortId(1L), 2L),
        ),
)
