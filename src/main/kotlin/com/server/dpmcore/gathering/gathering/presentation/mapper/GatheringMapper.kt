package com.server.dpmcore.gathering.gathering.presentation.mapper

import com.server.dpmcore.bill.bill.persentation.dto.request.CreateGatheringRequest
import com.server.dpmcore.bill.bill.persentation.dto.response.CreateGatheringResponse
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringCategory
import com.server.dpmcore.gathering.gatheringMember.presentation.mapper.GatheringMemberMapper.toCreateGatheringMemberResponse
import com.server.dpmcore.gathering.gatheringMember.presentation.mapper.GatheringMemberMapper.toGatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper.ReceiptMapper.toCreateReceiptResponse
import com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper.ReceiptMapper.toReceipt
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.LocalDateTime
import java.time.ZoneId

object GatheringMapper {
    fun toGathering(createGatheringRequest: CreateGatheringRequest): Gathering =
        Gathering(
            title = createGatheringRequest.title,
            description = createGatheringRequest.description,
            hostUserId = MemberId(createGatheringRequest.hostUserId),
            heldAt = createGatheringRequest.heldAt.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            category = GatheringCategory.valueOf(createGatheringRequest.category),
            roundNumber = createGatheringRequest.roundNumber,
            gatheringMembers =
                createGatheringRequest.gatheringMembers
                    ?.map { gatheringMember ->
                        toGatheringMember(gatheringMember)
                    }?.toMutableList() ?: mutableListOf(),
            receipt = toReceipt(createGatheringRequest.receipt),
        )

    fun toCreateGatheringResponse(gathering: Gathering): CreateGatheringResponse =
        CreateGatheringResponse(
            title = gathering.title,
            description = gathering.description,
            roundNumber = gathering.roundNumber,
            heldAt = LocalDateTime.ofInstant(gathering.heldAt, ZoneId.of("Asia/Seoul")),
            category = gathering.category.value,
            receipt = toCreateReceiptResponse(gathering.receipt!!),
            joinMemberCount = gathering.getGatheringJoinMemberCount(),
            amount = gathering.receipt!!.amount,
            gatheringMembers =
                gathering.gatheringMembers
                    .map { gatheringMember ->
                        toCreateGatheringMemberResponse(gatheringMember)
                    }.toMutableList(),
        )
}
