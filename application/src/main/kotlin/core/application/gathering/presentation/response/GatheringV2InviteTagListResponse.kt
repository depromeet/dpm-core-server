package core.application.gathering.presentation.response

import core.domain.gathering.enums.GatheringV2InviteTag

data class GatheringV2InviteTagListResponse(
    val inviteTags: List<GatheringV2InviteTagNameResponse>,
) {
    companion object {
        fun fromAllTags() =
            GatheringV2InviteTagListResponse(
                inviteTags = GatheringV2InviteTag.entries.map { GatheringV2InviteTagNameResponse.from(it) },
            )
    }
}
