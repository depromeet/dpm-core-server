package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotParticipantMemberException
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.application.exception.GatheringMemberNotFoundException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.GatheringMemberQueryUseCase
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringMemberQueryService(
    private val gatheringMemberPersistencePort: GatheringMemberPersistencePort,
    private val gatheringMemberValidator: GatheringMemberValidator,
) : GatheringMemberQueryUseCase {
    override fun getGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringMemberPersistencePort.findByGatheringId(gatheringId)

    fun getGatheringMemberByGatheringIdAndMemberId(
        gatheringId: GatheringId,
        memberId: MemberId,
    ): GatheringMember =
        gatheringMemberPersistencePort
            .findByGatheringIdAndMemberId(gatheringId, memberId)

    fun getMemberIdsByGatheringId(gatheringId: GatheringId): List<MemberId> =
        gatheringMemberPersistencePort
            .findMemberIdsByGatheringId(gatheringId)
            .takeIf { it.isNotEmpty() }
            ?: throw GatheringMemberNotFoundException()

    fun getQueryGatheringMemberIsJoined(gatheringId: GatheringId): List<GatheringMemberIsJoinQueryModel> {
        val memberIds = getMemberIdsByGatheringId(gatheringId)
        return memberIds.map { memberId ->
            val queryResults =
                gatheringMemberPersistencePort
                    .findGatheringMemberWithIsJoinByGatheringIdAndMemberId(gatheringId, memberId)
                    .let { queryResults ->
                        // TODO : 기수 정보가 추가됐을 때 기수 기준 정렬 등의 로직 추가 필요
                        if (queryResults.size > 1) {
                            queryResults.sortedWith(
                                compareBy {
                                    if (it.authority == AuthorityType.ORGANIZER.name) 0 else 1
                                },
                            )
                        } else {
                            queryResults
                        }
                    }
            queryResults.first()
        }
    }

    fun getQueryGatheringMemberIsInvitationSubmitted(
        gatheringId: GatheringId,
    ): List<BillMemberIsInvitationSubmittedQueryModel> {
        val memberIds = getMemberIdsByGatheringId(gatheringId)
        return memberIds.map { memberId ->
            var queryResults =
                gatheringMemberPersistencePort
                    .findGatheringMemberWithIsInvitationSubmittedByGatheringIdAndMemberId(gatheringId, memberId)
                    .let { queryResults ->
                        // TODO : 기수 정보가 추가됐을 때 기수 기준 정렬 등의 로직 추가 필요
                        if (queryResults.size > 1) {
                            queryResults.sortedWith(
                                compareBy {
                                    if (it.authority == AuthorityType.ORGANIZER.name) 0 else 1
                                },
                            )
                        } else {
                            queryResults
                        }
                    }
            queryResults.firstOrNull() ?: throw GatheringMemberNotFoundException()
        }
    }

    fun getGatheringMemberByGatheringIdsAndMemberIds(
        gatheringIds: List<GatheringId>,
        memberIds: List<MemberId>,
    ): List<GatheringMember> =
        gatheringMemberPersistencePort
            .findGatheringMembersByGatheringIdsAndMemberIds(gatheringIds, memberIds)

    /**
     * 각 회식 멤버가 회식 참여자에 속하는지 확인한 후, 참여자 수를 카운트합니다.
     *
     * @throws GatheringNotParticipantMemberException 회식 멤버가 해당 회식에 속하지 않는 경우
     * @author LeeHanEum
     * @since 2025.09.13
     */
    fun countGatheringParticipants(
        gatheringId: GatheringId,
        gatheringMembers: List<GatheringMember>,
    ): Int =
        gatheringMembers
            .onEach { gatheringMemberValidator.validateGatheringIdMatches(it, gatheringId) }
            .count { it.isJoined() }
}
