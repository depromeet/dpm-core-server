package com.server.dpmcore.bill.bill.persentation.dto.request

data class CreateGatheringMemberRequest(
    val memberId: Long,
    val isJoined: Boolean = true,
    val isCompleted: Boolean = false,
)
