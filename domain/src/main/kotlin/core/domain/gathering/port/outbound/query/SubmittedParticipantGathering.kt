package core.domain.gathering.port.outbound.query

import core.domain.gathering.vo.GatheringId

data class SubmittedParticipantGathering(
    val gatheringId: GatheringId,
    val isJoined: Boolean?,
)
