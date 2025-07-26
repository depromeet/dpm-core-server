package com.server.dpmcore.bill.bill.presentation.dto.response

data class CreateInviteAuthorityResponse(
    val inviteAuthorityId: Long = 1L,
    val authorityName: String = "17기",
    val authorityMemberCount: Long = 1L,
)
