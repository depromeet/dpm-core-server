package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillSummaryListByMemberResponse(
    val members: List<BillSummaryByMemberResponse>,
) {
    data class BillSummaryByMemberResponse(
        val name: String,
        val authority: String,
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

    companion object {
        fun from(members: List<BillSummaryByMemberResponse>): BillSummaryListByMemberResponse =
            BillSummaryListByMemberResponse(members)
    }
}
