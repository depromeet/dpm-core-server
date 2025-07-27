package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillDetailInviteAuthorityResponse(
    val inviteAuthorityId: Long = 1L,
    val authorityName: String = "17기 운영진",
    val authorityMemberCount: Int = 1,
) {
    companion object {
        fun defaultInviteAuthorityResponse(): List<BillDetailInviteAuthorityResponse> =
            listOf(
                BillDetailInviteAuthorityResponse(
                    inviteAuthorityId = 1L,
                    authorityName = "17기 운영진",
                    authorityMemberCount = 10,
                ),
                BillDetailInviteAuthorityResponse(
                    inviteAuthorityId = 2L,
                    authorityName = "17기 디퍼",
                    authorityMemberCount = 60,
                ),
            )
    }
}
