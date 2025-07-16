package com.server.dpmcore.bill.bill.persentation.dto.response

data class CreateGatheringMemberResponse(
    val memberId: Long,
    val name: String? = null,
    val isJoined: Boolean,
    val isCompleted: Boolean,
)
