package com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.command

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId

data class GatheringMemberDepositCommand(
    val gatheringMemberId: Long,
    val isDeposit: Boolean,
    val memo: String?,
) {
    companion object {
        fun of(
            gatheringMemberId: GatheringMemberId,
            isDeposit: Boolean,
            memo: String?,
        ) = GatheringMemberDepositCommand(
            gatheringMemberId = gatheringMemberId.value,
            isDeposit = isDeposit,
            memo = memo,
        )
    }
}
