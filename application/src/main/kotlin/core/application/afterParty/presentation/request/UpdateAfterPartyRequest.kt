package core.application.afterParty.presentation.request

import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.enums.AfterPartyCategory
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class UpdateAfterPartyRequest(
    val title: String,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val isApproved: Boolean,
    val authorMemberId: MemberId,
    val canEditAfterApproval: Boolean,
    val inviteTags: List<CreateAfterPartyInviteTagRequest>,
) {
    fun toDomain(afterPartyId: AfterPartyId): AfterParty =
        AfterParty(
            id = afterPartyId,
            title = title,
            description = description,
            category = AfterPartyCategory.AFTER_PARTY,
            scheduledAt = localDateTimeToInstant(scheduledAt),
            closedAt = localDateTimeToInstant(closedAt),
            isApproved = isApproved,
            authorMemberId = authorMemberId,
            canEditAfterApproval = canEditAfterApproval,
        )
}
