package com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.command

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId

data class GatheringMemberJoinCommand(
    val gatheringMemberId: Long,
    val isJoined: Boolean,
) {
    companion object {
        fun of(
            gatheringMemberId: GatheringMemberId,
            isJoined: Boolean,
        ) = GatheringMemberJoinCommand(
            gatheringMemberId = gatheringMemberId.value,
            isJoined = isJoined,
        )
    }
}
