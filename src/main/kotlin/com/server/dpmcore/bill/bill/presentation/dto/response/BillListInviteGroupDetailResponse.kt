package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillListInviteGroupDetailResponse(
    val inviteGroupId: Long,
    val groupName: String,
    val groupMemberCount: Long,
)
