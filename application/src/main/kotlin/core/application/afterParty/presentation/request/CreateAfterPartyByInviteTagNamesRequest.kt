package core.application.afterParty.presentation.request

import java.time.LocalDateTime

data class CreateAfterPartyByInviteTagNamesRequest(
    val title: String,
    val description: String,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val canEditAfterApproval: Boolean,
    val inviteTagNames: List<String>,
)
