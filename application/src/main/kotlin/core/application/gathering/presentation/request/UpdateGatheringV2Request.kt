package core.application.gathering.presentation.request

import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class UpdateGatheringV2Request(
    val title: String,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val closedAt: LocalDateTime,
    val isApproved: Boolean,
    val authorMemberId: MemberId,
    val canEditAfterApproval: Boolean,
    val inviteTags: List<CreateGatheringV2InviteTagRequest>,
) {
    fun toDomain(gatheringV2Id: GatheringV2Id): GatheringV2 =
        GatheringV2(
            id = gatheringV2Id,
            title = title,
            description = description,
            category = GatheringCategory.GATHERING,
            scheduledAt = localDateTimeToInstant(scheduledAt),
            closedAt = localDateTimeToInstant(closedAt),
            isApproved = isApproved,
            authorMemberId = authorMemberId,
            canEditAfterApproval = canEditAfterApproval,
        )
}
