package com.server.dpmcore.bill.bill.application.mapper

import com.server.dpmcore.bill.bill.presentation.dto.request.GatheringForBillCreateRequest
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringMemberCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.ReceiptCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.ReceiptPhotoCommand
import com.server.dpmcore.member.member.domain.model.MemberId

object BillGatheringMapper {
    fun GatheringForBillCreateRequest.toCommand(): GatheringCreateCommand =
        GatheringCreateCommand(
            title = this.title,
            description = this.description,
            hostUserId = MemberId(this.hostUserId),
            roundNumber = this.roundNumber,
            heldAt = this.heldAt,
            receipt =
                ReceiptCommand(
                    amount = this.receipt.amount,
                    photos =
                        this.receipt.receiptPhotos?.map {
                            ReceiptPhotoCommand(
                                receiptId = it.receiptId,
                                photoUrl = it.photoUrl,
                            )
                        } ?: emptyList(),
                ),
            members =
                this.gatheringMembers?.map {
                    GatheringMemberCommand(
                        memberId = it.memberId,
                        isJoined = it.isJoined,
                        isCompleted = it.isCompleted,
                    )
                } ?: emptyList(),
        )
}
