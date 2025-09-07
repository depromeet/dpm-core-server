package com.server.dpmcore.gathering.gathering.domain.port.inbound.command

data class GatheringParticipantCommand(
    val gatheringId: Long,
    val isParticipated: Boolean,
    val isConfirmed: Boolean,
)
