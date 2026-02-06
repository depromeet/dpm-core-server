package core.application.gathering.presentation.response

import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.vo.GatheringV2Id
import java.time.LocalDateTime

data class GatheringV2DetailResponse(
    val gatheringId: GatheringV2Id,
    val title: String,
    val isOwner: Boolean,
    val rsvpStatus: Boolean,
    val isAttended: Boolean?,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val isRsvpGoingCount: Int,
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
            rsvpStatus: Boolean,
            isAttended: Boolean?,
            isRsvpGoingCount: Int,
            inviteeCount: Int,
            attendanceCount: Int,
        ): GatheringV2DetailResponse =
            GatheringV2DetailResponse(
                gatheringId = gatheringV2.id!!,
                title = gatheringV2.title,
                isOwner = isOwner,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                description = gatheringV2.description,
                scheduledAt = instantToLocalDateTime(gatheringV2.scheduledAt),
                isRsvpGoingCount = isRsvpGoingCount,
                inviteeCount = inviteeCount,
                attendanceCount = attendanceCount,
                createdAt = instantToLocalDateTime(gatheringV2.createdAt!!),
                closedAt = instantToLocalDateTime(gatheringV2.closedAt),
                inviteTags = GatheringV2InviteTagListResponse(),
            )
    }
}
