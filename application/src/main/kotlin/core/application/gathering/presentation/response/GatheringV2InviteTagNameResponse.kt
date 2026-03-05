package core.application.gathering.presentation.response

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.CohortId

data class GatheringV2InviteTagNameResponse(
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
) {
    companion object {
        fun from(afterPartyInviteTag: AfterPartyInviteTag): GatheringV2InviteTagNameResponse =
            GatheringV2InviteTagNameResponse(
                cohortId = afterPartyInviteTag.cohortId,
                authorityId = afterPartyInviteTag.authorityId,
                tagName = afterPartyInviteTag.tagName,
            )

        fun from(afterPartyInviteTagEnum: AfterPartyInviteTagEnum): GatheringV2InviteTagNameResponse =
            GatheringV2InviteTagNameResponse(
                cohortId = afterPartyInviteTagEnum.cohortId,
                authorityId = afterPartyInviteTagEnum.authorityId,
                tagName = afterPartyInviteTagEnum.tagName,
            )
    }
}
