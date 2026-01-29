package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.aggregate.Member
import core.domain.member.vo.MemberId

interface GatheringV2InviteeCommandUseCase {
    fun createGatheringV2Invitee(
        gatheringV2Invitee: GatheringV2Invitee,
        gatheringV2: GatheringV2,
        authorMember: Member,
        inviteeMember: Member,
    )

    /**
     * 참가 여부 제출하는 메서드입니다
     *
     * @param isRsvpGoing 참가 여부
     * @param memberId 변경하는 멤버 아이디
     * @param gatheringV2Id 회식 아이디
     * @return Unit
     *
     * @since 2026-01-26
     * @author junwon
     */
    fun submitGatheringV2RsvpStatus(
        isRsvpGoing: Boolean,
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
    )
}
