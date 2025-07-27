package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillListInviteAuthorityDetailResponse(
    val invitedAuthorityId: Long,
    val authorityName: String,
    val authorityMemberCount: Int,
) {
    companion object {
        fun defaultInviteAuthorityResponse(): List<BillListInviteAuthorityDetailResponse> =
            listOf(
                BillListInviteAuthorityDetailResponse(
                    invitedAuthorityId = 1L,
                    authorityName = "17기 운영진",
                    authorityMemberCount = 10,
                ),
                BillListInviteAuthorityDetailResponse(
                    invitedAuthorityId = 2L,
                    authorityName = "17기 디퍼",
                    authorityMemberCount = 60,
                ),
            )
    }
}
