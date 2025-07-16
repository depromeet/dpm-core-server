package com.server.dpmcore.bill.bill.persentation.dto.response

data class CreateInviteGroupResponse(
    val inviteGroupId: Long = 1L,
    val groupName: String = "17기",
    val groupMemberCount: Long = 1L,
)
