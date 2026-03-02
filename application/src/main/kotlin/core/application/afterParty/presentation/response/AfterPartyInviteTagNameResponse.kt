package core.application.afterParty.presentation.response

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.CohortId

data class AfterPartyInviteTagNameResponse(
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
) {
    companion object {
        fun from(afterPartyInviteTag: AfterPartyInviteTag): AfterPartyInviteTagNameResponse =
            AfterPartyInviteTagNameResponse(
                cohortId = afterPartyInviteTag.cohortId,
                authorityId = afterPartyInviteTag.authorityId,
                tagName = afterPartyInviteTag.tagName,
            )

        fun from(afterPartyInviteTagEnum: AfterPartyInviteTagEnum): AfterPartyInviteTagNameResponse =
            AfterPartyInviteTagNameResponse(
                cohortId = afterPartyInviteTagEnum.cohortId,
                authorityId = afterPartyInviteTagEnum.authorityId,
                tagName = afterPartyInviteTagEnum.tagName,
            )
    }
}
