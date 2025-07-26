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
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BillMapper(
    private val gatheringQueryUseCase: GatheringQueryUseCase,
) {
    fun toBillDetailResponse(bill: Bill): BillDetailResponse {
        val gatherings =
            gatheringQueryUseCase.getAllGatheringsByBillId(bill.id ?: throw BillException.BillNotFoundException())

        val gatheringReceipt =
            gatherings.map { gathering ->
                gatheringQueryUseCase
                    .findGatheringReceiptByGatheringId(
                        gathering.id ?: throw GatheringException.GatheringNotFoundException(),
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
            gatherings =
                gatherings.map { gathering ->

                    val gatheringMembers =
                        gatheringQueryUseCase.findGatheringMemberByGatheringId(
                            gathering.id
                                ?: throw GatheringException.GatheringNotFoundException(),
                        )
                    BillDetailGatheringResponse.from(gathering, gatheringMembers)
                },
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
            gatheringQueryUseCase.getAllGatheringsByBillId(bill.id ?: throw BillException.BillNotFoundException()).map {
                toBillListGatheringDetailResponse(it)
            }
        if (gatheringDetails.isEmpty()) throw GatheringException.GatheringRequiredException()

        val gatheringMembers =
            gatheringQueryUseCase.findGatheringMemberByGatheringId(gatheringDetails.get(0).gatheringId)

        val billTotalAmount =
            gatheringDetails.sumOf { it.amount }

        return BillListDetailResponse(
            title = bill.title,
            billId = bill.id?.value ?: throw BillException.BillNotFoundException(),
            description = bill.description,
            billTotalAmount = billTotalAmount,
            billStatus = bill.billStatus,
            createdAt =
                instantToLocalDateTime(bill.createdAt ?: throw BillException.BillNotFoundException()),
            billAccountId = bill.billAccount.id?.value ?: throw BillAccountException.BillAccountNotFoundException(),
//            inviteAuthorities = TODO(),
            gatherings = gatheringDetails,
//            TODO : 매번 카운트 호출 좀 별로라서 고민해보면 좋을 것 같아요.
            invitedMemberCount = gatheringMembers.count(),
            invitationConfirmedCount = gatheringMembers.count { it.isInvitationConfirmed },
            invitationCheckedMemberCount = gatheringMembers.count { it.isChecked },
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
