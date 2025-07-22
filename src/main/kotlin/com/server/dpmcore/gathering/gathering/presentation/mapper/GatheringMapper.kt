package com.server.dpmcore.gathering.gathering.presentation.mapper

import com.server.dpmcore.bill.bill.presentation.dto.response.CreateGatheringResponse
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringMember.presentation.mapper.GatheringMemberMapper.toCreateGatheringMemberResponse
import com.server.dpmcore.gathering.gatheringReceipt.presentation.mapper.ReceiptMapper.toCreateReceiptResponse
import java.time.LocalDateTime
import java.time.ZoneId

object GatheringMapper {
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
