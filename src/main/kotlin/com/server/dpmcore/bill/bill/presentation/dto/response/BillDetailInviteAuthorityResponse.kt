package com.server.dpmcore.bill.bill.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class BillDetailInviteAuthorityResponse(
    @field:Schema(
        description = "정산 초대 대상 그룹 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val inviteAuthorityId: Long = 1L,
    @field:Schema(
        description = "정산 초대 대상 그룹 이름",
        example = "17_DEEPER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authorityName: String = "17_DEEPER",
    @field:Schema(
        description = "정산 초대 대상 그룹의 멤버 수",
        example = "64",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authorityMemberCount: Int = 1,
) {
    companion object {
        fun defaultInviteAuthorityResponse(): List<BillDetailInviteAuthorityResponse> =
            listOf(
                BillDetailInviteAuthorityResponse(
                    inviteAuthorityId = 1L,
                    authorityName = "17_DEEPER",
                    authorityMemberCount = 64,
                ),
                BillDetailInviteAuthorityResponse(
                    inviteAuthorityId = 2L,
                    authorityName = "17_ORGANIZER",
                    authorityMemberCount = 12,
                ),
            )
    }
}
