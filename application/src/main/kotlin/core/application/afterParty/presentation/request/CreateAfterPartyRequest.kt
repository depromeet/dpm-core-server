package core.application.afterParty.presentation.request

import core.application.gathering.presentation.request.CreateGatheringV2InviteTagRequest
import java.time.LocalDateTime

data class CreateAfterPartyRequest(
    val title: String,
    val description: String,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val canEditAfterApproval: Boolean,
    val inviteTags: List<CreateGatheringV2InviteTagRequest>,
)
