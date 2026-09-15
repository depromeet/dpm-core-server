package core.application.gathering.application.service.member

import core.application.gathering.application.exception.member.GatheringMemberNotFoundException
import core.application.gathering.application.validator.GatheringMemberValidator
import core.application.member.application.service.role.CurrentCohortRoleResolver
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
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
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
            val representativeAuthority =
                currentCohortRoleResolver.selectRepresentativeRoleForMember(
                    memberId = memberId,
                    roleNames = queryResults.map { it.authority },
                ) ?: queryResults.first().authority
            queryResults.first { it.authority == representativeAuthority }
        }
    }

    fun getQueryGatheringMemberIsInvitationSubmitted(
        gatheringId: GatheringId,
    ): List<BillMemberIsInvitationSubmittedQueryModel> {
        val memberIds = getMemberIdsByGatheringId(gatheringId)
        return memberIds.map { memberId ->
            val queryResults =
                gatheringMemberPersistencePort
                    .findGatheringMemberWithIsInvitationSubmittedByGatheringIdAndMemberId(gatheringId, memberId)
            val representativeAuthority =
                currentCohortRoleResolver.selectRepresentativeRoleForMember(
                    memberId = memberId,
                    roleNames = queryResults.map { it.authority },
                ) ?: queryResults.first().authority
            queryResults.firstOrNull { it.authority == representativeAuthority }
                ?: throw GatheringMemberNotFoundException()
        }
    }

    fun getGatheringMemberByGatheringIdsAndMemberIds(
        gatheringIds: List<GatheringId>,
        memberIds: List<MemberId>,
    ): List<GatheringMember> =
        gatheringMemberPersistencePort
            .findGatheringMembersByGatheringIdsAndMemberIds(gatheringIds, memberIds)

    fun countGatheringParticipants(
        gatheringId: GatheringId,
        gatheringMembers: List<GatheringMember>,
    ): Int =
        gatheringMembers
            .onEach { gatheringMemberValidator.validateGatheringIdMatches(it, gatheringId) }
            .count { it.isJoined() }
}
