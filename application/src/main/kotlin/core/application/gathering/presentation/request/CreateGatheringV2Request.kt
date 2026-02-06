package core.application.gathering.presentation.request

import java.time.LocalDateTime

data class CreateGatheringV2Request(
    val title: String,
    val description: String,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val canEditAfterApproval: Boolean,
    val inviteTags: List<CreateGatheringV2InviteTagRequest>,
)
