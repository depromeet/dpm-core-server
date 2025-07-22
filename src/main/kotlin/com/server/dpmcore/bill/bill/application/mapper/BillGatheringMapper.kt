package com.server.dpmcore.bill.bill.application.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.GatheringForBillCreateRequest
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.ReceiptCommand
import com.server.dpmcore.member.member.domain.model.MemberId

object BillGatheringMapper {
    fun GatheringForBillCreateRequest.toCommand(hostUserId: MemberId): GatheringCreateCommand =
        GatheringCreateCommand(
            title = this.title,
            description = this.description,
            hostUserId = hostUserId,
            roundNumber = this.roundNumber,
            heldAt = this.heldAt,
            receipt =
                ReceiptCommand(
                    amount = this.receipt.amount,
                ),
        )
}
