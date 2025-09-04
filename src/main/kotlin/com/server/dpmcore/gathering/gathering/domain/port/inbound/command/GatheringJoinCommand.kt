package com.server.dpmcore.gathering.gathering.domain.port.inbound.command

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId

data class GatheringJoinCommand(
    val gatheringMemberId: Long,
    val isJoined: Boolean,
) {
    companion object {
        fun of(
            gatheringMemberId: GatheringMemberId,
            isJoined: Boolean,
        ) = GatheringJoinCommand(
            gatheringMemberId = gatheringMemberId.value,
            isJoined = isJoined,
        )
    }
}
