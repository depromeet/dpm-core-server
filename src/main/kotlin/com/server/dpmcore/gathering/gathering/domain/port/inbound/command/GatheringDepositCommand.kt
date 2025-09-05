package com.server.dpmcore.gathering.gathering.domain.port.inbound.command

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId

data class GatheringDepositCommand(
    val gatheringMemberId: Long,
    val isDeposit: Boolean,
    val memo: String?,
) {
    companion object {
        fun of(
            gatheringMemberId: GatheringMemberId,
            isDeposit: Boolean,
            memo: String?,
        ) = GatheringDepositCommand(
            gatheringMemberId = gatheringMemberId.value,
            isDeposit = isDeposit,
            memo = memo,
        )
    }
}
