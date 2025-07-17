package com.server.dpmcore.bill.bill.presentation.dto.request

data class CreateGatheringMemberRequest(
    val memberId: Long,
    val isJoined: Boolean = true,
    val isCompleted: Boolean = false,
)
