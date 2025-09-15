package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel
import com.server.dpmcore.gathering.gathering.application.exception.GatheringIdRequiredException
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotFoundException
import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotParticipantMemberException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.domain.model.query.SubmittedParticipantGathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.port.inbound.query.GatheringMemberReceiptQueryModel
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gathering.presentation.response.GatheringMemberJoinListResponse
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberQueryService
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query.GatheringMemberIsJoinQueryModel
import com.server.dpmcore.gathering.gatheringReceipt.application.GatheringReceiptQueryService
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringQueryService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val gatheringReceiptQueryService: GatheringReceiptQueryService,
    private val gatheringMemberQueryService: GatheringMemberQueryService,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val gatheringValidator: GatheringValidator,
) : GatheringQueryUseCase {
    override fun getAllGatheringsByBillId(billId: BillId): List<Gathering> =
        gatheringPersistencePort
            .findByBillId(billId)

    override fun getAllGatheringIdsByBillId(billId: BillId): List<GatheringId> =
        gatheringPersistencePort.findAllGatheringIdsByBillId(billId)

    override fun findGatheringReceiptByGatheringId(gatheringId: GatheringId): GatheringReceipt =
        gatheringReceiptQueryService
            .findByGatheringId(gatheringId)

    override fun findGatheringMemberByGatheringId(gatheringId: GatheringId): List<GatheringMember> =
        gatheringMemberQueryService
            .getGatheringMemberByGatheringId(gatheringId)

    override fun getGatheringMemberReceiptByBillId(billId: BillId): List<GatheringMemberReceiptQueryModel> {
        val gatheringIds = getAllGatheringIdsByBillId(billId)
        val mappedIds = mappingMemberIdsByGatheringIds(gatheringIds)

        return mappedIds.map { (memberId, gatheringIds) ->
            val (memberName, authority) = getMemberNameAuthority(memberId)
//            TODO : 여기 멤버에 해당하는 gathering의 분할 금액을 넘겨야하지 않은가..?
            val totalSplitAmount = findTotalSplitAmount(gatheringIds)

            GatheringMemberReceiptQueryModel.of(
                memberName = memberName,
                memberAuthority = authority,
                memberSplitAmount = totalSplitAmount,
            )
        }
    }

    override fun getSubmittedParticipantEachGathering(
        billId: BillId,
        memberId: MemberId,
    ): List<SubmittedParticipantGathering> =
        gatheringPersistencePort.getSubmittedParticipantEachGathering(
            billId,
            memberId,
        )

    fun findById(gatheringId: Long) =
        gatheringPersistencePort
            .findById(gatheringId)

    fun getGatheringMemberJoinList(gatheringId: GatheringId): GatheringMemberJoinListResponse {
        val query: List<GatheringMemberIsJoinQueryModel> =
            gatheringMemberQueryService.getQueryGatheringMemberIsJoined(gatheringId)
        return GatheringMemberJoinListResponse(query)
    }

    private fun mappingMemberIdsByGatheringIds(gatheringIds: List<GatheringId>): Map<MemberId, List<GatheringId>> =
        gatheringIds
            .flatMap { gatheringId ->
                gatheringMemberQueryService
                    .getMemberIdsByGatheringId(gatheringId)
                    .map { memberId -> memberId to gatheringId }
            }.groupBy({ it.first }, { it.second })

    private fun getMemberNameAuthority(memberId: MemberId): Pair<String, String> {
        val queryResults =
            memberQueryUseCase
                .getMemberNameAuthorityByMemberId(memberId)
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

        return queryResults.first().let { it.name to it.authority }
    }

    override fun findTotalSplitAmount(gatheringIds: List<GatheringId>): Int? {
        var totalSplitAmount = 0

        gatheringIds.forEach { gatheringId ->
            val splitAmount = gatheringReceiptQueryService.getSplitAmountByGatheringId(gatheringId)

            if (splitAmount == null) {
                return null
            } else {
                totalSplitAmount += splitAmount
            }
        }

        return totalSplitAmount
    }

    override fun getBillMemberSubmittedList(billId: BillId): List<BillMemberIsInvitationSubmittedQueryModel> {
        val gatheringIds = getAllGatheringIdsByBillId(billId)
        return gatheringMemberQueryService.getQueryGatheringMemberIsInvitationSubmitted(
            gatheringIds.firstOrNull()
                ?: throw GatheringNotFoundException(),
        )
    }

    override fun getAllGatheringMembersByBillId(billId: BillId): List<GatheringMember> =
        getAllGatheringIdsByBillId(billId).flatMap { gatheringId ->
            gatheringMemberQueryService.getGatheringMemberByGatheringId(gatheringId)
        }

    /**
     * 회식 정산서 정보 반환 시, 회식 참여 인원 수를 반환하는 메서드 입니다.
     *
     * @param gathering 회식 정보
     * @param gatheringMembers 해당 회식에 속한 회식 멤버 리스트
     * @return 회식에 참여한 멤버 수
     * @throws GatheringIdRequiredException 회식 ID가 null인 경우
     * @throws GatheringNotParticipantMemberException 회식 멤버가 해당 회식에 속하지 않는 경우
     * @author LeeHanEum
     * @since 2025.09.13
     */
    override fun getGatheringJoinMemberCount(
        gathering: Gathering,
        gatheringMembers: List<GatheringMember>,
    ): Int {
        val gatheringId = gatheringValidator.validateGatheringIdIsNotNull(gathering)
        return gatheringMemberQueryService.countGatheringParticipants(gatheringId, gatheringMembers)
    }
}
