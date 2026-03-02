package core.application.gathering.presentation.response

import core.application.afterParty.application.exception.AfterPartyNotFoundException
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class GatheringV2DetailResponse(
    val gatheringId: AfterPartyId,
    val title: String,
    val isOwner: Boolean,
    val rsvpStatus: Boolean?,
    val isAttended: Boolean?,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val submitRsvpCount: Int,
    val rsvpGoingCount: Int,
    val notRsvpGoingCount: Int,
    val inviteeCount: Int,
    val attendanceCount: Int,
    val authorMemberId: MemberId,
    val createdAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val canEditAfterApproval: Boolean,
    val isClosed: Boolean,
    val inviteTags: GatheringV2InviteTagListResponse,
) {
    companion object {
        fun of(
            afterParty: AfterParty,
            isOwner: Boolean,
            rsvpStatus: Boolean?,
            isAttended: Boolean?,
            submitRsvpCount: Int,
            rsvpGoingCount: Int,
            notRsvpGoingCount: Int,
            inviteeCount: Int,
            attendanceCount: Int,
            isClosed: Boolean,
            inviteTags: List<AfterPartyInviteTag> = emptyList(),
        ): GatheringV2DetailResponse =
            GatheringV2DetailResponse(
                gatheringId = afterParty.id!!,
                title = afterParty.title,
                isOwner = isOwner,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                description = afterParty.description,
                scheduledAt = instantToLocalDateTime(afterParty.scheduledAt),
                submitRsvpCount = submitRsvpCount,
                rsvpGoingCount = rsvpGoingCount,
                notRsvpGoingCount = notRsvpGoingCount,
                inviteeCount = inviteeCount,
                attendanceCount = attendanceCount,
                authorMemberId = afterParty.authorMemberId,
                createdAt = instantToLocalDateTime(afterParty.createdAt ?: throw AfterPartyNotFoundException()),
                closedAt = instantToLocalDateTime(afterParty.closedAt),
                canEditAfterApproval = afterParty.canEditAfterApproval,
                isClosed = isClosed,
                inviteTags =
                    GatheringV2InviteTagListResponse(
                        inviteTags = inviteTags.map { GatheringV2InviteTagNameResponse.from(it) },
                    ),
            )
    }
}
