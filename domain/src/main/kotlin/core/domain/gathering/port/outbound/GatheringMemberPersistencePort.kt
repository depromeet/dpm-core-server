package core.domain.gathering.port.outbound

import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.port.outbound.query.GatheringMemberIsJoinQueryModel
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId

interface GatheringMemberPersistencePort {
    fun save(
        gatheringMember: GatheringMember,
        gathering: Gathering,
    )

    fun findByGatheringId(gatheringId: GatheringId): List<GatheringMember>

    fun findByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): GatheringMember

    fun findMemberIdsByGatheringId(gatheringId: GatheringId): List<MemberId>

    fun findGatheringMemberWithIsJoinByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<GatheringMemberIsJoinQueryModel>

    fun findGatheringMemberWithIsInvitationSubmittedByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): List<BillMemberIsInvitationSubmittedQueryModel>

    fun findGatheringMembersByGatheringIdsAndMemberIds(
        gatheringIds: List<GatheringId>,
        memberIds: List<MemberId>,
    ): List<GatheringMember>

    fun updateIsViewedById(memberId: Long)

    fun updateIsJoinedById(gatheringMember: GatheringMember)

    fun updateDepositAndMemoById(
        gatheringMember: GatheringMember,
        isDeposit: Boolean,
        memo: String?,
    )

    fun updateIsInvitationSubmittedById(memberId: Long)
}
