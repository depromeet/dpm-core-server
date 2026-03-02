package core.application.afterParty.presentation.response

import core.application.afterParty.application.exception.AfterPartyNotFoundException
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class AfterPartyListResponse(
    val afterPartyId: AfterPartyId,
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
            afterParty: AfterParty,
            memberId: MemberId,
            rsvpStatus: Boolean?,
            isAttended: Boolean?,
            isRsvpGoingCount: Int,
            isAttendedCount: Int,
            inviteeCount: Int,
        ): AfterPartyListResponse =
            AfterPartyListResponse(
                afterPartyId = afterParty.id ?: throw AfterPartyNotFoundException(),
                title = afterParty.title,
                isOwner = afterParty.authorMemberId == memberId,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                isApproved = afterParty.isApproved,
                description = afterParty.description,
                scheduledAt = instantToLocalDateTime(afterParty.scheduledAt),
                closedAt = instantToLocalDateTime(afterParty.closedAt),
                isRsvpGoingCount = isRsvpGoingCount,
                isAttendedCount = isAttendedCount,
                inviteeCount = inviteeCount,
                createdAt = instantToLocalDateTime(afterParty.createdAt) ?: throw AfterPartyNotFoundException(),
            )
    }
}
