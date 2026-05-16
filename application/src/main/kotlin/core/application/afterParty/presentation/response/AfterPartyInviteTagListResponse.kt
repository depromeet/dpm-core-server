package core.application.afterParty.presentation.response

import core.domain.member.enums.InviteTagEnum

data class AfterPartyInviteTagListResponse(
    val inviteTags: List<AfterPartyInviteTagNameResponse>,
) {
    companion object {
        fun fromAllTags() =
            AfterPartyInviteTagListResponse(
                inviteTags = InviteTagEnum.entries.map { AfterPartyInviteTagNameResponse.from(it) },
            )
    }
}
