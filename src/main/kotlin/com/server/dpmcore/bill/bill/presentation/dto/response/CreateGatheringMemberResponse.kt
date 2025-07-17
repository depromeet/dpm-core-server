package com.server.dpmcore.bill.bill.presentation.dto.response

data class CreateGatheringMemberResponse(
    val memberId: Long,
    val name: String? = null,
    val isJoined: Boolean,
    val isCompleted: Boolean,
)
