package com.server.dpmcore.bill.bill.persentation.mapper

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.persentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.persentation.dto.response.CreateBillResponse
import com.server.dpmcore.bill.bill.persentation.dto.response.CreateInviteGroupResponse
import com.server.dpmcore.bill.billAccount.persentation.mapper.BillAccountMapper.toBillAccount
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.presentation.mapper.GatheringMapper.toCreateGatheringResponse
import com.server.dpmcore.gathering.gathering.presentation.mapper.GatheringMapper.toGathering
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.LocalDateTime
import java.time.ZoneId

object BillMapper {
    fun toBill(createBillRequest: CreateBillRequest): Bill =
        Bill(
            title = createBillRequest.title,
            hostUserId = MemberId(createBillRequest.hostUserId),
            billAccount = toBillAccount(createBillRequest.billAccountId),
            description = createBillRequest.description,
            gatherings =
                createBillRequest.gatherings
                    .map { gathering ->
                        toGathering(gathering)
                    }.toMutableList(),
        )

    fun toCreateBillResponse(bill: Bill): CreateBillResponse =
        CreateBillResponse(
            billId = bill.id?.value ?: throw BillException.BillIdRequiredException(),
            title = bill.title,
            description = bill.description,
            hostUserId = bill.hostUserId.value,
            billTotalAmount = bill.gatherings.sumOf { gathering -> gathering.receipt!!.amount }.toLong(),
            heldAt = LocalDateTime.ofInstant(bill.gatherings.get(0).heldAt, ZoneId.of("Asia/Seoul")),
            billAccountId = bill.billAccount.id?.value ?: throw BillException.BillAccountIdRequiredException(),
//            TODO : 회식 참여 가능 인원 태그 추가
            inviteGroups = mutableListOf(),
            gatherings = bill.gatherings.map { gathering -> toCreateGatheringResponse(gathering) }.toMutableList(),
        )

    fun toCreateInviteGroupResponse(): CreateInviteGroupResponse = CreateInviteGroupResponse()
}
