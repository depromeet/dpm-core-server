package core.application.gathering.presentation.response

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class GatheringV2ListResponse(
    val gatheringId: GatheringV2Id,
    val title: String,
    val isOwner: Boolean,
    val isParticipated: Boolean?,
    val isApproved: Boolean,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val joinCount: Int,
    val inviteeCount: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            gatheringV2: GatheringV2,
            gatheringV2Invitees: List<GatheringV2Invitee>,
            isParticipated: Boolean?,
            memberId: MemberId,
        ): GatheringV2ListResponse =
            GatheringV2ListResponse(
                gatheringId = gatheringV2.id ?: throw GatheringNotFoundException(),
                title = gatheringV2.title,
                isOwner = gatheringV2.authorMemberId == memberId,
                isParticipated = isParticipated,
                isApproved = gatheringV2.isApproved,
                description = gatheringV2.description,
                scheduledAt = instantToLocalDateTime(gatheringV2.scheduledAt),
                closedAt = instantToLocalDateTime(gatheringV2.closedAt),
                joinCount = gatheringV2Invitees.count { it.isParticipating() },
                inviteeCount = gatheringV2Invitees.count(),
                createdAt = instantToLocalDateTime(gatheringV2.createdAt) ?: throw GatheringNotFoundException(),
            )
    }
}
