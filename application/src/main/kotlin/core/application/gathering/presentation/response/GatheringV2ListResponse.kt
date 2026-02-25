package core.application.gathering.presentation.response

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.gathering.application.exception.GatheringNotFoundException
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class GatheringV2ListResponse(
    val gatheringId: GatheringV2Id,
    val title: String,
    val isOwner: Boolean,
    val rsvpStatus: Boolean?,
    val isAttended: Boolean?,
    val isApproved: Boolean,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val isRsvpGoingCount: Int,
    val isAttendedCount: Int,
    val inviteeCount: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            gatheringV2: GatheringV2,
            memberId: MemberId,
            rsvpStatus: Boolean?,
            isAttended: Boolean?,
            isRsvpGoingCount: Int,
            isAttendedCount: Int,
            inviteeCount: Int,
        ): GatheringV2ListResponse =
            GatheringV2ListResponse(
                gatheringId = gatheringV2.id ?: throw GatheringNotFoundException(),
                title = gatheringV2.title,
                isOwner = gatheringV2.authorMemberId == memberId,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                isApproved = gatheringV2.isApproved,
                description = gatheringV2.description,
                scheduledAt = instantToLocalDateTime(gatheringV2.scheduledAt),
                closedAt = instantToLocalDateTime(gatheringV2.closedAt),
                isRsvpGoingCount = isRsvpGoingCount,
                isAttendedCount = isAttendedCount,
                inviteeCount = inviteeCount,
                createdAt = instantToLocalDateTime(gatheringV2.createdAt) ?: throw GatheringNotFoundException(),
            )
    }
}
