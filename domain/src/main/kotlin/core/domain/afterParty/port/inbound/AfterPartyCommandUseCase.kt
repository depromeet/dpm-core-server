package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.InviteTagEnum
import core.domain.member.vo.MemberId

interface AfterPartyCommandUseCase {
    fun createAfterParty(
        afterParty: AfterParty,
        afterPartyInviteTags: List<InviteTagEnum>,
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

    fun sendNotificationUnMarkedRsvp(
        afterPartyId: AfterPartyId,
        title: String,
        body: String,
    )
}
