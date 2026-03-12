package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId

interface AfterPartyCommandUseCase {
    fun createAfterParty(
        afterParty: AfterParty,
        afterPartyInviteTags: List<AfterPartyInviteTagEnum>,
        authorMemberId: MemberId,
    )

    fun createAfterPartyByInviteTagNames(
        afterParty: AfterParty,
        inviteTagNames: List<String>,
        authorMemberId: MemberId,
    )

    fun updateAfterParty(afterParty: AfterParty)

    fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    )

    fun compensateMissingInviteesForOpenAfterParties(): Int
}
