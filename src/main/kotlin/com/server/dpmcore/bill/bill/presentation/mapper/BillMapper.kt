package com.server.dpmcore.bill.bill.presentation.mapper

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailGatheringResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListGatheringDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.exception.BillAccountException
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.exception.GatheringException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BillMapper(
    private val gatheringQueryUseCase: GatheringQueryUseCase,
) {
    fun toBillDetailResponse(
        bill: Bill,
        memberId: MemberId,
    ): BillDetailResponse {
        val gatherings =
            gatheringQueryUseCase.getAllGatheringsByBillId(bill.id ?: throw BillException.BillNotFoundException())
        if (gatherings.isEmpty()) throw GatheringException.GatheringRequiredException()

        val gatheringReceipt =
            gatherings.map { gathering ->
                gatheringQueryUseCase
                    .findGatheringReceiptByGatheringId(
                        gathering.id ?: throw GatheringException.GatheringNotFoundException(),
                    )
            }

        val allBillGatheringMembers =
            gatheringQueryUseCase.findGatheringMemberByGatheringId(
                gatherings.get(0).id ?: throw GatheringException.GatheringIdRequiredException(),
            )

        val gatheringMembersByRetrievedMember = allBillGatheringMembers.filter { it.memberId == memberId }

        return BillDetailResponse(
            billId = bill.id ?: throw BillException.BillNotFoundException(),
            title = bill.title,
            description = bill.description,
            hostUserId = bill.hostUserId.value,
            billTotalAmount = bill.getBillTotalAmount(gatheringReceipt),
            billTotalSplitAmount = bill.getMemberBillSplitAmount(gatheringMembersByRetrievedMember, gatheringReceipt),
            billStatus = bill.billStatus,
            createdAt =
                LocalDateTime.ofInstant(
                    bill.createdAt,
                    ZoneId.of(TIME_ZONE),
                ),
            billAccountId = bill.billAccount.id?.value ?: 0L,
//            TODO : 매번 카운트 호출 좀 별로라서 고민해보면 좋을 것 같아요.
            invitedMemberCount = allBillGatheringMembers.count(),
            invitationSubmittedCount = allBillGatheringMembers.count { it.isInvitationSubmitted },
            invitationCheckedMemberCount = allBillGatheringMembers.count { it.isChecked },
            isViewed = bill.getIsBillViewed(gatheringMembersByRetrievedMember),
            isJoined = bill.getIsBillJoined(gatheringMembersByRetrievedMember),
            isInvitationSubmitted = bill.getIsBillInvitationSubmitted(gatheringMembersByRetrievedMember),
            gatherings =
                gatherings.map { gathering ->

                    val gatheringMembers =
                        gatheringQueryUseCase.findGatheringMemberByGatheringId(
                            gathering.id
                                ?: throw GatheringException.GatheringNotFoundException(),
                        )
                    val splitAmount =
                        gatheringReceipt
                            .find {
                                it.gatheringId == gathering.id
                            }?.splitAmount ?: 0

                    BillDetailGatheringResponse.from(gathering, gatheringMembers, splitAmount)
                },
        )
    }

    fun toBillListResponse(
        bills: List<Bill>,
        memberId: MemberId,
    ): BillListResponse {
        val billDetailResponses =
            bills
                .map {
                    toBillListDetailResponse(it, memberId)
                }.toList()

        return BillListResponse(
            bills = billDetailResponses,
        )
    }

    fun toBillListDetailResponse(
        bill: Bill,
        memberId: MemberId,
    ): BillListDetailResponse {
        val gatheringDetails =
            gatheringQueryUseCase.getAllGatheringsByBillId(bill.id ?: throw BillException.BillNotFoundException()).map {
                toBillListGatheringDetailResponse(it)
            }
        if (gatheringDetails.isEmpty()) throw GatheringException.GatheringRequiredException()

//        TODO : 여기서 모든 gathering에 대해서 조회해서 체크하는 로직 필요(각 gathering마다 멤버가 다를 수 있어서)
        val allBillGatheringMembers =
            gatheringQueryUseCase.findGatheringMemberByGatheringId(gatheringDetails.get(0).gatheringId)

        val gatheringMembersByRetrievedMember = allBillGatheringMembers.filter { it.memberId == memberId }

        val participants: MutableMap<Long, MutableList<Long>> = mutableMapOf<Long, MutableList<Long>>()

        gatheringDetails.forEach { gatheringDetail ->
            gatheringQueryUseCase
                .findGatheringMemberByGatheringId(gatheringDetail.gatheringId)
                .filter { it.isJoined }
                .forEach { gatheringMember ->
                    participants
                        .computeIfAbsent(gatheringMember.memberId!!.value) { mutableListOf() }
                        .add(gatheringDetail.gatheringId.value)
                }
        }
        val participantCount = participants.filter { it.value.isNotEmpty() }.count()

        val billTotalAmount =
            gatheringDetails.sumOf { it.amount }

        return BillListDetailResponse(
            title = bill.title,
            billId = bill.id ?: throw BillException.BillNotFoundException(),
            description = bill.description,
            billTotalAmount = billTotalAmount,
            billStatus = bill.billStatus,
            createdAt =
                instantToLocalDateTime(bill.createdAt ?: throw BillException.BillNotFoundException()),
            billAccountId = bill.billAccount.id?.value ?: throw BillAccountException.BillAccountNotFoundException(),
//            TODO : 매번 카운트 호출 좀 별로라서 고민해보면 좋을 것 같아요.
            invitedMemberCount = allBillGatheringMembers.count(),
            invitationSubmittedCount = allBillGatheringMembers.count { it.isInvitationSubmitted },
            invitationCheckedMemberCount = allBillGatheringMembers.count { it.isChecked },
            participantCount = participantCount,
            isViewed = bill.getIsBillViewed(gatheringMembersByRetrievedMember),
            isJoined = bill.getIsBillJoined(gatheringMembersByRetrievedMember),
            isInvitationSubmitted = bill.getIsBillInvitationSubmitted(gatheringMembersByRetrievedMember),
//            inviteAuthorities = TODO(),
            gatherings = gatheringDetails,
        )
    }

    fun toBillListGatheringDetailResponse(gathering: Gathering): BillListGatheringDetailResponse {
        val gatheringReceipt =
            gatheringQueryUseCase.findGatheringReceiptByGatheringId(
                gathering.id ?: throw GatheringException.GatheringNotFoundException(),
            )
        val gatheringMembers = gatheringQueryUseCase.findGatheringMemberByGatheringId(gathering.id)
        val joinMemberCount = gatheringMembers.count { it.isJoined }

        return BillListGatheringDetailResponse(
            gatheringId = gathering.id,
            title = gathering.title,
            description = gathering.description,
            roundNumber = gathering.roundNumber,
            heldAt =
                gathering.heldAt
                    .atZone(ZoneId.of(TIME_ZONE))
                    .toLocalDateTime(),
            category = gathering.category,
//            receipt = gatheringReceipt, TODO : 영수증 추 후 구현
            joinMemberCount = joinMemberCount,
            amount = gatheringReceipt.amount,
            splitAmount = gatheringReceipt.splitAmount ?: 0,
        )
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}
