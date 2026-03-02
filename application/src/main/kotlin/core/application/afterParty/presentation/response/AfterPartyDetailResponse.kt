package core.application.afterParty.presentation.response

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class AfterPartyDetailResponse(
    val afterPartyId: AfterPartyId,
    val title: String,
    val isOwner: Boolean,
    val rsvpStatus: Boolean?,
    val isAttended: Boolean?,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val isRsvpGoingCount: Int,
    val inviteeCount: Int,
    val attendanceCount: Int,
    val authorMemberId: MemberId,
    val createdAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val isClosed: Boolean,
    val inviteTags: AfterPartyInviteTagListResponse,
) {
    companion object {
        fun of(
            afterParty: AfterParty,
            isOwner: Boolean,
            rsvpStatus: Boolean?,
            isAttended: Boolean?,
            isRsvpGoingCount: Int,
            inviteeCount: Int,
            attendanceCount: Int,
            isClosed: Boolean,
            inviteTags: List<AfterPartyInviteTag> = emptyList(),
        ): AfterPartyDetailResponse =
            AfterPartyDetailResponse(
                afterPartyId = afterParty.id!!,
                title = afterParty.title,
                isOwner = isOwner,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                description = afterParty.description,
                scheduledAt = instantToLocalDateTime(afterParty.scheduledAt),
                isRsvpGoingCount = isRsvpGoingCount,
                inviteeCount = inviteeCount,
                attendanceCount = attendanceCount,
                authorMemberId = afterParty.authorMemberId,
                createdAt = instantToLocalDateTime(afterParty.createdAt!!),
                closedAt = instantToLocalDateTime(afterParty.closedAt),
                isClosed = isClosed,
                inviteTags =
                    AfterPartyInviteTagListResponse(
                        inviteTags = inviteTags.map { AfterPartyInviteTagNameResponse.from(it) },
                    ),
            )
    }
}
