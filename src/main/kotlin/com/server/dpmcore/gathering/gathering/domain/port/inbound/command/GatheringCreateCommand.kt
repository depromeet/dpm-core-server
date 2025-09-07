package com.server.dpmcore.gathering.gathering.domain.port.inbound.command

import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.LocalDateTime

data class GatheringCreateCommand(
    val title: String,
    val description: String?,
    val hostUserId: MemberId,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val receipt: ReceiptCommand,
)
