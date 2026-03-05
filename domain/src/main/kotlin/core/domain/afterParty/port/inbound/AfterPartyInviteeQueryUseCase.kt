package core.domain.afterParty.port.inbound

import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId

interface AfterPartyInviteeQueryUseCase {
    fun getInviteesByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInvitee>

    fun getInviteeByMemberIdAndAfterPartyId(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): AfterPartyInvitee
}
