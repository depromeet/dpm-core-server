package core.application.afterParty.presentation.response

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.InviteTagEnum

data class AfterPartyInviteTagNameResponse(
    val cohortId: CohortId,
    val authorityId: AuthorityId,
    val tagName: String,
) {
    companion object {
        fun from(afterPartyInviteTag: AfterPartyInviteTag): AfterPartyInviteTagNameResponse =
            AfterPartyInviteTagNameResponse(
                cohortId = afterPartyInviteTag.cohortId,
                authorityId = afterPartyInviteTag.authorityId,
                tagName = afterPartyInviteTag.tagName,
            )

        fun from(inviteTagEnum: InviteTagEnum): AfterPartyInviteTagNameResponse =
            AfterPartyInviteTagNameResponse(
                cohortId = inviteTagEnum.cohortId,
                authorityId = inviteTagEnum.authorityId,
                tagName = inviteTagEnum.tagName,
            )
    }
}
