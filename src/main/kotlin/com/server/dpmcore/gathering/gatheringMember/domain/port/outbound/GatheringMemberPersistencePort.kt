package com.server.dpmcore.gathering.gatheringMember.domain.port.outbound

import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringDepositCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringJoinCommand
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel
import com.server.dpmcore.member.member.domain.model.MemberId

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

    fun updateIsViewedById(gatheringMemberId: Long)

    fun updateIsJoinedById(command: GatheringJoinCommand)

    fun updateDepositAndMemoById(command: GatheringDepositCommand)

    fun updateIsInvitationSubmittedById(memberId: Long)
}
