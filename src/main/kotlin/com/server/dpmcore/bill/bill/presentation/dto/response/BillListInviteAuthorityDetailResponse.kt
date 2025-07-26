package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillListInviteAuthorityDetailResponse(
    val invitedAuthorityId: Long,
    val authorityName: String,
    val authorityMemberCount: Long,
)
