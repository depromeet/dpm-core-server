package com.server.dpmcore.bill.bill.presentation.mapper

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailGatheringResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListGatheringDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringQueryUseCase
import com.server.dpmcore.gathering.gatheringMember.domain.port.GatheringMemberQueryUseCase
import com.server.dpmcore.gathering.gatheringReceipt.domain.port.GatheringReceiptQueryUseCase
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BillMapper(
    private val gatheringQueryUseCase: GatheringQueryUseCase,
    private val gatheringReceiptQueryUseCase: GatheringReceiptQueryUseCase,
    private val gatheringMemberQueryUseCase: GatheringMemberQueryUseCase,
) {
    fun toBillDetailResponse(bill: Bill): BillDetailResponse {
        val gatherings = gatheringQueryUseCase.getAllGatheringsByGatheringIds(bill.gatheringIds)
        val gatheringReceipt =
            gatherings.map { gathering ->
                gatheringReceiptQueryUseCase
                    .findByGatheringId(
                        gathering.id ?: throw BillException.GatheringNotFoundException(),
                    )
            }

//        TODO : 자기가 참여한 모임만 합산해야함.
        val billTotalAmount = gatheringReceipt.sumOf { it.amount }
        val billTotalSplitAmount = gatheringReceipt.sumOf { it.splitAmount ?: 0 }

        return BillDetailResponse(
            title = bill.title,
            description = bill.description,
            hostUserId = bill.hostUserId.value,
            billTotalAmount = billTotalAmount,
            billTotalSplitAmount = billTotalSplitAmount,
            billStatus = bill.billStatus,
            createdAt =
                LocalDateTime.ofInstant(
                    bill.createdAt,
                    ZoneId.of(TIME_ZONE),
                ),
            billAccountId = bill.billAccount.id?.value ?: 0L,
            gatherings = gatherings.map { BillDetailGatheringResponse.from(it) },
        )
    }

    fun toBillListResponse(bills: List<Bill>): BillListResponse {
        val billDetailResponses =
            bills
                .map {
                    toBillListDetailResponse(it)
                }.toList()

        return BillListResponse(
            bills = billDetailResponses,
        )
    }

    fun toBillListDetailResponse(bill: Bill): BillListDetailResponse {
        val gatheringDetails =
            gatheringQueryUseCase.getAllGatheringsByGatheringIds(bill.gatheringIds).map {
                toBillListGatheringDetailResponse(it)
            }
        val billTotalAmount =
            gatheringDetails.sumOf { it.amount }

        return BillListDetailResponse(
            title = bill.title,
            billId = bill.id?.value ?: throw BillException.BillNotFoundException(),
            description = bill.description,
            billTotalAmount = billTotalAmount,
            billStatus = bill.billStatus,
            createdAt =
                bill.createdAt
                    ?.atZone(ZoneId.of(TIME_ZONE))
                    ?.toLocalDateTime() ?: throw BillException.BillNotFoundException(),
            billAccountId = bill.billAccount.id?.value ?: throw BillException.BillAccountNotFoundException(),
//            inviteGroups = TODO(),
//            answerMemberCount = TODO(),
            gatherings = gatheringDetails,
        )
    }

    fun toBillListGatheringDetailResponse(gathering: Gathering): BillListGatheringDetailResponse {
        val gatheringReceipt =
            gatheringReceiptQueryUseCase.findByGatheringId(
                gathering.id ?: throw BillException.GatheringNotFoundException(),
            )
        val gatheringMembers = gatheringMemberQueryUseCase.getGatheringMemberByGatheringId(gathering.id)
        val joinMemberCount = gatheringMembers.count { it.isJoined }

        return BillListGatheringDetailResponse(
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
