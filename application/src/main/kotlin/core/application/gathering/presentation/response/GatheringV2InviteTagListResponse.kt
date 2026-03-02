package core.application.gathering.presentation.response

import core.domain.afterParty.enums.AfterPartyInviteTagEnum

data class GatheringV2InviteTagListResponse(
    val inviteTags: List<GatheringV2InviteTagNameResponse>,
) {
    companion object {
        fun fromAllTags() =
            GatheringV2InviteTagListResponse(
                inviteTags = AfterPartyInviteTagEnum.entries.map { GatheringV2InviteTagNameResponse.from(it) },
            )
    }
}
