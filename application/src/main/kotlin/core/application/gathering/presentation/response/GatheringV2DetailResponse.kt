package core.application.gathering.presentation.response

import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.vo.GatheringV2Id
import java.time.LocalDateTime

data class GatheringV2DetailResponse(
    val gatheringId: GatheringV2Id,
    val title: String,
    val isOwner: Boolean,
    val isRsvpGoing: Boolean,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val participantCount: Int,
    val inviteeCount: Int,
    val attendanceCount: Int,
    val createdAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val inviteTags: GatheringV2InviteTagListResponse,
) {
    companion object {
        fun of(
            gatheringV2: GatheringV2,
            isOwner: Boolean,
            isRsvpGoing: Boolean,
            participantCount: Int,
            inviteeCount: Int,
            attendanceCount: Int,
        ): GatheringV2DetailResponse =
            GatheringV2DetailResponse(
                gatheringId = gatheringV2.id!!,
                title = gatheringV2.title,
                isOwner = isOwner,
                isRsvpGoing = isRsvpGoing,
                description = gatheringV2.description,
                scheduledAt = instantToLocalDateTime(gatheringV2.scheduledAt),
                participantCount = participantCount,
                inviteeCount = inviteeCount,
                attendanceCount = attendanceCount,
                createdAt = instantToLocalDateTime(gatheringV2.createdAt!!),
                closedAt = instantToLocalDateTime(gatheringV2.closedAt),
                inviteTags = GatheringV2InviteTagListResponse(),
            )
    }
}
