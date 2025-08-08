package com.server.dpmcore.bill.bill.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class BillSummaryListByMemberResponse(
    val members: List<BillSummaryByMemberResponse>,
) {
    data class BillSummaryByMemberResponse(
        @field:Schema(
            description = "멤버 이름",
            example = "정준원",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val name: String,
        @field:Schema(
            description = "권한 이름",
            example = "17_ORGANIZER",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val authority: String,
        @field:Schema(
            description = "멤버가 분할한 금액",
            example = "50000",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val splitAmount: Int,
    ) {
        companion object {
            fun of(
                name: String,
                authority: String,
                splitAmount: Int,
            ): BillSummaryByMemberResponse =
                BillSummaryByMemberResponse(
                    name = name,
                    authority = authority,
                    splitAmount = splitAmount,
                )
        }
    }
}
