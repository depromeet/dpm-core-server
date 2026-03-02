package core.application.afterParty.presentation.response

import core.domain.afterParty.enums.AfterPartyInviteTagEnum

data class AfterPartyInviteTagListResponse(
    val inviteTags: List<AfterPartyInviteTagNameResponse>,
) {
    companion object {
        fun fromAllTags() =
            AfterPartyInviteTagListResponse(
                inviteTags = AfterPartyInviteTagEnum.entries.map { AfterPartyInviteTagNameResponse.from(it) },
            )
    }
}
