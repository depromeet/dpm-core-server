package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId

interface AfterPartyInviteeCommandUseCase {
    fun createAfterPartyInvitee(
        afterPartyInvitee: AfterPartyInvitee,
        afterParty: AfterParty,
        authorMember: Member,
        inviteeMember: Member,
    )

    /**
     * 참가 여부 제출하는 메서드입니다
     *
     * @param isRsvpGoing 참가 여부
     * @param memberId 변경하는 멤버 아이디
     * @param afterPartyId 회식 아이디
     * @return Unit
     *
     * @since 2026-01-26
     * @author junwon
     */
    fun submitAfterPartyRsvpStatus(
        isRsvpGoing: Boolean,
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    )
}
