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

    @Deprecated("회식은 현재 MVP에서 사용하지 않습니다. 2차 MVP 검토 전까지 기존 승인 초기화 동작을 유지합니다.")
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
