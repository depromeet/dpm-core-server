package core.application.gathering.application.service.member

import core.application.gathering.application.exception.member.GatheringMemberNotFoundException
import core.application.gathering.application.validator.GatheringMemberValidator
import core.domain.authority.enums.AuthorityType
import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.port.inbound.GatheringMemberQueryUseCase
import core.domain.gathering.port.outbound.GatheringMemberPersistencePort
import core.domain.gathering.port.outbound.query.GatheringMemberIsJoinQueryModel
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId
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
            ?: throw GatheringMemberNotFoundException()

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
