package core.domain.gathering.port.inbound

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.member.vo.MemberId

interface GatheringV2CommandUseCase {
    fun createGatheringV2(
        gatheringV2: GatheringV2,
        gatheringV2InviteTags: List<GatheringV2InviteTag>,
        authorMemberId: MemberId,
    )

    fun createGatheringV2ByInviteTagNames(
        gatheringV2: GatheringV2,
        inviteTagNames: List<String>,
        authorMemberId: MemberId,
    )

    fun updateGatheringV2(gatheringV2: GatheringV2)
}
