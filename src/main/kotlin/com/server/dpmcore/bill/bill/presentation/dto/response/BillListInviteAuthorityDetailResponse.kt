package com.server.dpmcore.bill.bill.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class BillListInviteAuthorityDetailResponse(
    @field:Schema(
        description = "정산 초대 대상 그룹 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val invitedAuthorityId: Long,
    @field:Schema(
        description = "정산 초대 대상 그룹 이름",
        example = "DEEPER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authorityName: String,
    @field:Schema(
        description = "정산 초대 대상 그룹의 멤버 수",
        example = "64",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
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
