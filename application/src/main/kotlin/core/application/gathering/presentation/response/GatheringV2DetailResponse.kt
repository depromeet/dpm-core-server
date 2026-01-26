package core.application.gathering.presentation.response

import core.domain.gathering.vo.GatheringV2Id
import java.time.LocalDateTime

data class GatheringV2DetailResponse(
    val gatheringId: GatheringV2Id,
    val title: String,
    val isOwner: Boolean,
    val isJoined: Boolean,
    val description: String,
    val scheduledAt: LocalDateTime,
    val participantCount: Int,
    val inviteeCount: Int,
    val attendanceCount: Int,
    val createdAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val inviteTags: List<GatheringV2InviteTagListResponse>,
)
