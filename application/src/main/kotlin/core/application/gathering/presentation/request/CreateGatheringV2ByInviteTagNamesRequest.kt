package core.application.gathering.presentation.request

import java.time.LocalDateTime

data class CreateGatheringV2ByInviteTagNamesRequest(
    val title: String,
    val description: String,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val canEditAfterApproval: Boolean,
    val inviteTagNames: List<String>,
)
