package core.application.bill.presentation.mapper

import core.application.bill.application.exception.BillNotFoundException
import core.application.bill.application.exception.account.BillAccountNotFoundException
import core.application.bill.presentation.response.BillDetailGatheringResponse
import core.application.bill.presentation.response.BillDetailResponse
import core.application.bill.presentation.response.BillListDetailResponse
import core.application.bill.presentation.response.BillListGatheringDetailResponse
import core.application.bill.presentation.response.BillListResponse
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.application.exception.GatheringRequiredException
import core.application.gathering.application.exception.receipt.GatheringReceiptNotFoundException
import core.domain.bill.aggregate.Bill
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.port.inbound.GatheringQueryUseCase
import core.domain.member.vo.MemberId
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
        val billId = bill.id ?: throw BillNotFoundException()

        val gatherings =
            gatheringQueryUseCase.getAllGatheringsByBillId(billId)
        if (gatherings.isEmpty()) throw GatheringRequiredException()

        val gatheringReceipt =
            gatherings.map { gathering ->
                gatheringQueryUseCase
                    .findGatheringReceiptByGatheringId(
                        gathering.id ?: throw GatheringNotFoundException(),
                    )
            }

        val allBillGatheringMembers =
            gatheringQueryUseCase.getAllGatheringMembersByBillId(billId)

        val gatheringMembersByRetrievedMember = allBillGatheringMembers.filter { it.memberId == memberId }

        val billTotalSplitAmount =
            gatheringQueryUseCase.findTotalSplitAmount(
                gatherings.map {
                    it.id ?: throw GatheringNotFoundException()
                },
            )
        val myTotalSplitAmount: Int =
            Bill.findMemberBillTotalSplitAmount(
                memberId,
                gatheringMembersByRetrievedMember,
                gatheringReceipt,
            ) ?: throw GatheringReceiptNotFoundException()

        return BillDetailResponse(
            billId = billId,
            title = bill.title,
            description = bill.description,
            hostUserId = bill.hostUserId.value,
            billTotalAmount = Bill.getBillTotalAmount(gatheringReceipt),
            billTotalSplitAmount = billTotalSplitAmount,
            myTotalSplitAmount = myTotalSplitAmount,
            billStatus = bill.billStatus,
            createdAt =
                LocalDateTime.ofInstant(
                    bill.createdAt,
                    ZoneId.of(TIME_ZONE),
                ),
            billAccountId = bill.billAccount.id?.value ?: 0L,
//            TODO : 매번 카운트 호출 좀 별로라서 고민해보면 좋을 것 같아요.
            invitedMemberCount = Bill.getBillInvitedMemberCount(allBillGatheringMembers),
            invitationSubmittedCount = Bill.getBillInvitationSubmittedCount(allBillGatheringMembers),
            invitationCheckedMemberCount = Bill.getBillInvitationCheckedMemberCount(allBillGatheringMembers),
            isViewed = Bill.getIsBillViewed(gatheringMembersByRetrievedMember),
            isJoined = Bill.getIsBillJoined(gatheringMembersByRetrievedMember),
            isInvitationSubmitted = Bill.getIsBillInvitationSubmitted(gatheringMembersByRetrievedMember),
            gatherings =
                gatherings.map { gathering ->

                    val gatheringMembers =
                        gatheringQueryUseCase.findGatheringMemberByGatheringId(
                            gathering.id
                                ?: throw GatheringNotFoundException(),
                        )
                    val splitAmount = gatheringReceipt.find { it.gatheringId == gathering.id }?.splitAmount
                    val joinMemberCount = gatheringQueryUseCase.getGatheringJoinMemberCount(gathering, gatheringMembers)
                    BillDetailGatheringResponse.of(gathering, joinMemberCount, splitAmount)
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
        val billId = bill.id ?: throw BillNotFoundException()

        val gatheringDetails =
            gatheringQueryUseCase.getAllGatheringsByBillId(billId).map {
                toBillListGatheringDetailResponse(it)
            }
        if (gatheringDetails.isEmpty()) throw GatheringRequiredException()

//        TODO : 여기서 모든 gathering에 대해서 조회해서 체크하는 로직 필요(각 gathering마다 멤버가 다를 수 있어서)
        val allBillGatheringMembers =
            gatheringQueryUseCase.getAllGatheringMembersByBillId(billId)

        val gatheringMembersByRetrievedMember = allBillGatheringMembers.filter { it.memberId == memberId }

        val participants: MutableMap<Long, MutableList<Long>> = mutableMapOf<Long, MutableList<Long>>()

        gatheringDetails.forEach { gatheringDetail ->
            gatheringQueryUseCase
                .findGatheringMemberByGatheringId(gatheringDetail.gatheringId)
                .filter { it.isJoined == true }
                .forEach { gatheringMember ->
                    participants
                        .computeIfAbsent(gatheringMember.memberId.value) { mutableListOf() }
                        .add(gatheringDetail.gatheringId.value)
                }
        }
        val participantCount = participants.filter { it.value.isNotEmpty() }.count()

        val billTotalAmount =
            gatheringDetails.sumOf { it.amount }

        return BillListDetailResponse(
            title = bill.title,
            billId = billId,
            description = bill.description,
            billTotalAmount = billTotalAmount,
            billStatus = bill.billStatus,
            createdAt =
                instantToLocalDateTime(bill.createdAt ?: throw BillNotFoundException()),
            billAccountId = bill.billAccount.id?.value ?: throw BillAccountNotFoundException(),
//            TODO : 매번 카운트 호출 좀 별로라서 고민해보면 좋을 것 같아요.
            invitedMemberCount = Bill.getBillInvitedMemberCount(allBillGatheringMembers),
            invitationSubmittedCount = Bill.getBillInvitationSubmittedCount(allBillGatheringMembers),
            invitationCheckedMemberCount = Bill.getBillInvitationCheckedMemberCount(allBillGatheringMembers),
            participantCount = participantCount,
            isViewed = Bill.getIsBillViewed(gatheringMembersByRetrievedMember),
            isJoined = Bill.getIsBillJoined(gatheringMembersByRetrievedMember),
            isInvitationSubmitted = Bill.getIsBillInvitationSubmitted(gatheringMembersByRetrievedMember),
//            inviteAuthorities = TODO(),
            gatherings = gatheringDetails,
        )
    }

    fun toBillListGatheringDetailResponse(gathering: Gathering): BillListGatheringDetailResponse {
        val gatheringId = gathering.id ?: throw GatheringNotFoundException()

        val gatheringReceipt =
            gatheringQueryUseCase.findGatheringReceiptByGatheringId(
                gathering.id ?: throw GatheringNotFoundException(),
            )
        val gatheringMembers = gatheringQueryUseCase.findGatheringMemberByGatheringId(gatheringId)
        val joinMemberCount = gatheringMembers.count { it.isJoined == true }

        return BillListGatheringDetailResponse(
            gatheringId = gatheringId,
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
            splitAmount = gatheringReceipt.splitAmount,
        )
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}
