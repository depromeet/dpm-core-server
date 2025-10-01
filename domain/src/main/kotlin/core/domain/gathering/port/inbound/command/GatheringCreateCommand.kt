package core.domain.gathering.port.inbound.command

import core.domain.member.vo.MemberId
import java.time.LocalDateTime

data class GatheringCreateCommand(
    val title: String,
    val description: String?,
    val hostUserId: MemberId,
    val roundNumber: Int,
    val heldAt: LocalDateTime,
    val receipt: ReceiptCommand,
)
