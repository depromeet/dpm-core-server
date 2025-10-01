package core.domain.gathering.port.inbound.command

import core.domain.gathering.vo.GatheringId

data class JoinGatheringCommand(
    val gatheringId: GatheringId,
    val isJoined: Boolean,
)
